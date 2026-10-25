package com.armod.identifier.ar

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import com.google.ar.core.Coordinates2d
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Custom [GLSurfaceView] that renders the live ARCore camera background feed.
 *
 * Integrates an OpenGL ES 2.0 pipeline that:
 * 1. Allocates an external OpenGL OES texture for ARCore camera frames.
 * 2. Binds the texture name to the active ARCore session.
 * 3. Updates display geometry based on screen orientation and dimensions.
 * 4. Calls Session.update on every frame to pull the camera image and transforms UV coordinates.
 * 5. Passes the live Frame to ArSessionManager for live tracking status updates.
 */
class ArCameraSurfaceView @JvmOverloads constructor(
    context: Context,
    private val sessionManager: ArSessionManager,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private val renderer = ArCameraRenderer(context, sessionManager)

    init {
        preserveEGLContextOnPause = true
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        setZOrderMediaOverlay(true)
        setWillNotDraw(false)
    }
}

internal class ArCameraRenderer(
    private val context: Context,
    private val sessionManager: ArSessionManager
) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "ArCameraRenderer"

        private const val VERTEX_SHADER_CODE = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            void main() {
                gl_Position = a_Position;
                v_TexCoord = a_TexCoord;
            }
        """

        private const val FRAGMENT_SHADER_CODE = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 v_TexCoord;
            uniform samplerExternalOES u_Texture;
            void main() {
                gl_FragColor = texture2D(u_Texture, v_TexCoord);
            }
        """

        // Quad covering the entire screen in NDC: 4 vertices (x, y)
        private val QUAD_COORDS = floatArrayOf(
            -1.0f, -1.0f,
             1.0f, -1.0f,
            -1.0f,  1.0f,
             1.0f,  1.0f
        )
    }

    private var program = 0
    private var positionHandle = 0
    private var texCoordHandle = 0
    private var textureHandle = 0
    private var textureId = -1
    private var isTextureSetInSession = false

    private var viewportWidth = 1
    private var viewportHeight = 1
    private var viewportChanged = false

    private val quadBuffer: FloatBuffer = ByteBuffer.allocateDirect(QUAD_COORDS.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(QUAD_COORDS)
            position(0)
        }

    private val texCoordBuffer: FloatBuffer = ByteBuffer.allocateDirect(8 * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    private val transformedTexCoords = FloatArray(8)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // 1. Generate external OES texture
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        // 2. Build OpenGL shaders and program
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(it, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val error = GLES20.glGetProgramInfoLog(it)
                GLES20.glDeleteProgram(it)
                Log.e(TAG, "Shader program link failed: $error")
            }
        }

        positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
        texCoordHandle = GLES20.glGetAttribLocation(program, "a_TexCoord")
        textureHandle = GLES20.glGetUniformLocation(program, "u_Texture")

        isTextureSetInSession = false
        Log.i(TAG, "GL surface created. Texture ID: $textureId")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        viewportChanged = true
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val session = sessionManager.session ?: return
        if (textureId < 0) return

        try {
            // Register texture name with ARCore if not yet bound
            if (!isTextureSetInSession) {
                session.setCameraTextureName(textureId)
                isTextureSetInSession = true
                Log.i(TAG, "Bound camera texture $textureId to ARCore Session.")
            }

            // Update display geometry on orientation/size changes
            if (viewportChanged) {
                val rotation = getDisplayRotation()
                session.setDisplayGeometry(rotation, viewportWidth, viewportHeight)
                viewportChanged = false
            }

            // Pull latest camera frame
            val frame = session.update()

            // Update live tracking and session state from the real frame
            sessionManager.updateFromFrame(frame)

            // Transform UV coordinates to properly orient and aspect-fit camera frame
            frame.transformCoordinates2d(
                Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                QUAD_COORDS,
                Coordinates2d.TEXTURE_NORMALIZED,
                transformedTexCoords
            )
            texCoordBuffer.clear()
            texCoordBuffer.put(transformedTexCoords)
            texCoordBuffer.position(0)

            // Render camera texture
            GLES20.glUseProgram(program)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
            GLES20.glUniform1i(textureHandle, 0)

            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, quadBuffer)

            GLES20.glEnableVertexAttribArray(texCoordHandle)
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

            GLES20.glDisableVertexAttribArray(positionHandle)
            GLES20.glDisableVertexAttribArray(texCoordHandle)

        } catch (e: Exception) {
            Log.w(TAG, "Error rendering AR camera frame: ${e.message}")
        }
    }

    @Suppress("DEPRECATION")
    private fun getDisplayRotation(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.rotation ?: Surface.ROTATION_0
        } else {
            (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
                ?.defaultDisplay?.rotation ?: Surface.ROTATION_0
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val error = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            Log.e(TAG, "Shader compilation error: $error")
        }
        return shader
    }
}
