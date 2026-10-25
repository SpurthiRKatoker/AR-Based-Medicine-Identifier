# Dataset Provenance and Source Documentation

**Project:** SLAM-Based AR Medicine Identifier with Voice-Guided Accessibility Mode  
**Document version:** 1.0  
**Dataset file:** `app/src/main/assets/medicines.json`  
**Dataset size:** 25 records  
**Access / curation date:** 2026-10-17  
**Dataset version:** 1.0  

---

## 1. Governance and Provenance Policy
In strict compliance with **DECISIONS.md (D-005, D-012)** and **AR_Medicine_Identifier_Backend_Schema.md**:
1. **Academic Proof-of-Concept Scope:** This dataset is curated exclusively for testing on-device OCR, field extraction, local matching, and accessibility presentation. It does not provide clinical decision support or personalized medical advice.
2. **Zero Fabrication:** Every record is derived from publicly accessible government or regulatory health agency monographs. No facts, indications, or warnings were generated or invented.
3. **Traceability:** Every record includes an auditable `sourceTitle`, `sourceUrl`, and `sourceAccessDate`.

---

## 2. Primary Data Sources
1. **National List of Essential Medicines (NLEM 2022)**  
   - Publisher: Ministry of Health and Family Welfare (MoHFW), Government of India  
   - URL: https://main.mohfw.gov.in/sites/default/files/NLEM%202022.pdf  
   - Purpose: Baseline generic ingredient names, authorized strengths, dosage forms, and essential medicine categorization.
2. **NHS UK Medicines A to Z**  
   - Publisher: National Health Service, United Kingdom  
   - URL: https://www.nhs.uk/medicines/  
   - License / Terms: Open Government Licence (OGL v3.0) for public health information.  
   - Purpose: Standard label indications, patient-facing warnings, common adverse effects, and administration instructions.
3. **DailyMed / FDA Drug Labels**  
   - Publisher: National Library of Medicine (NLM) / US Food and Drug Administration (FDA)  
   - URL: https://dailymed.nlm.nih.gov/  
   - Terms: Public domain official label database.
4. **WHO Model List of Essential Medicines (23rd List, 2023)**  
   - Publisher: World Health Organization (WHO)  
   - URL: https://www.who.int/publications/i/item/WHO-MHP-HPS-EML-2023.02  
   - Purpose: Cross-validation of international generic names and standard strengths.

---

## 3. Curated Medicine Catalogue Summary (25 Records)

| ID | Brand Name | Generic Name | Strength | Dosage Form | Primary Sourced Reference |
|---|---|---|---|---|---|
| `med-nlem-001` | Crocin | Paracetamol | 500 mg | Tablet | NLEM 2022 / NHS UK Paracetamol |
| `med-nlem-002` | Dolo 650 | Paracetamol | 650 mg | Tablet | NLEM 2022 / CDSCO |
| `med-nlem-003` | Cetzine | Cetirizine Hydrochloride | 10 mg | Tablet | NHS UK Cetirizine / NLEM 2022 |
| `med-nlem-004` | Brufen | Ibuprofen | 400 mg | Tablet | NHS UK Ibuprofen / NLEM 2022 |
| `med-nlem-005` | Novamox | Amoxicillin | 500 mg | Capsule | WHO EML / NHS UK Amoxicillin |
| `med-nlem-006` | Pan 40 | Pantoprazole | 40 mg | Tablet | DailyMed FDA Pantoprazole / NLEM 2022 |
| `med-nlem-007` | Omez | Omeprazole | 20 mg | Capsule | NHS UK Omeprazole / NLEM 2022 |
| `med-nlem-008` | Azithral | Azithromycin | 500 mg | Tablet | WHO Model Formulary / DailyMed FDA |
| `med-nlem-009` | Glycomet | Metformin Hydrochloride | 500 mg | Tablet | NHS UK Metformin / NLEM 2022 |
| `med-nlem-010` | Amlong | Amlodipine Besylate | 5 mg | Tablet | NHS UK Amlodipine / NLEM 2022 |
| `med-nlem-011` | Atorva | Atorvastatin Calcium | 10 mg | Tablet | NHS UK Atorvastatin / NLEM 2022 |
| `med-nlem-012` | Atorva 20 | Atorvastatin Calcium | 20 mg | Tablet | DailyMed FDA Atorvastatin / NLEM 2022 |
| `med-nlem-013` | Ciplox | Ciprofloxacin Hydrochloride | 500 mg | Tablet | NHS UK Ciprofloxacin / WHO EML 2023 |
| `med-nlem-014` | Voveran | Diclofenac Sodium | 50 mg | Tablet | DailyMed FDA Diclofenac / NLEM 2022 |
| `med-nlem-015` | Vomitop | Domperidone | 10 mg | Tablet | NHS UK Domperidone / CDSCO |
| `med-nlem-016` | Flagyl | Metronidazole | 400 mg | Tablet | NHS UK Metronidazole / WHO EML |
| `med-nlem-017` | Zentel | Albendazole | 400 mg | Chewable Tablet | WHO EML / DailyMed FDA Albendazole |
| `med-nlem-018` | Doxylab | Doxycycline | 100 mg | Capsule | NHS UK Doxycycline / NLEM 2022 |
| `med-nlem-019` | Losar | Losartan Potassium | 50 mg | Tablet | NHS UK Losartan / NLEM 2022 |
| `med-nlem-020` | Betaloc | Metoprolol Tartrate | 50 mg | Tablet | NHS UK Metoprolol / NLEM 2022 |
| `med-nlem-021` | Asthalin | Salbutamol Sulfate | 4 mg | Tablet | NHS UK Salbutamol / NLEM 2022 |
| `med-nlem-022` | Thyronorm | Levothyroxine Sodium | 50 mcg | Tablet | NHS UK Levothyroxine / DailyMed FDA |
| `med-nlem-023` | Avil | Pheniramine Maleate | 25 mg | Tablet | CDSCO / NLEM 2022 |
| `med-nlem-024` | Electral | Oral Rehydration Salts | 21.8 g | Powder | WHO EML / UNICEF Guidelines |
| `med-nlem-025` | Celin | Ascorbic Acid | 500 mg | Chewable Tablet | NHS UK Vitamin C / DailyMed FDA |
| `med-nlem-026` | Moxidose CV 625 | Amoxycillin and Clavulanic Acid | 625 mg | Tablet | NLEM 2022 / CDSCO |
