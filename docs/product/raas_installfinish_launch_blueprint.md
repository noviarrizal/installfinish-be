# RaaS Launch Blueprint — InstallFinish Ops

**Versi:** 1.0  
**Tanggal riset:** 9 November 2026  
**Pasar awal:** United States  
**Vertical:** residential HVAC installation contractors  
**Model:** Results-as-a-Service / managed operations  
**Status nama:** working name, belum legal/trademark/domain-cleared

---

# 0. Keputusan strategi

## Produk yang dibangun

**InstallFinish Ops — HVAC Installation Closeout Desk**

> We take HVAC installation admin off your team's plate — permits, inspection coordination, warranty registration, and job closeout — with every step tracked and evidenced.

InstallFinish bukan SaaS permitting tool, bukan AI receptionist, dan bukan CRM. InstallFinish adalah layanan operasional yang mengambil alih pekerjaan administrasi setelah sebuah HVAC installation sold/scheduled/completed.

### Kenapa bukan Lead Recovery Engine yang sebelumnya dibahas?

Lead recovery tetap punya demand, tetapi kategori missed-call text-back, AI receptionist, dan automated lead follow-up sudah ramai. CallbackOps menawarkan missed-call recovery sekitar US$149/bulan; FieldGain menawarkan bundled trade lead/revenue follow-up sekitar US$397/bulan; Jobber menambahkan receptionist AI ke produk field-service mereka. ReclaimOps dan ServiceLoop juga sudah masuk area recovery/booking. Fitur SMS balik setelah missed call saja terlalu mudah dikomoditisasi.

Permitting juga bukan ruang kosong. PermitFlow, PermitOps, PermitLane, PermitPilot, dan pemain lain sudah menyediakan software atau layanan seputar permit. Karena itu InstallFinish tidak akan menjual “AI permit software”. Wedge kita adalah **managed HVAC installation closeout operations**, dengan pekerjaan manusia + internal tooling + AI untuk menyelesaikan proses yang customer tidak ingin tangani sendiri.

### Bukti demand yang paling relevan

- Air Around the Clock di Deerfield Beach, Florida, memasang lowongan Permit Coordinator pada 26 Oktober 2026. Deskripsi lowongan menyebut target sekitar **10 permit per hari**, mengelola aplikasi lintas municipality, follow-up dokumen, dan mengatur inspeksi. Ini satu bukti perusahaan tertentu, bukan rata-rata industri.
- Must Air USA di Hallandale Beach mengiklankan HVAC Permit Processor & Purchasing Coordinator dengan tugas submit permit, scheduling inspection, warranty processing, equipment purchasing, dan tracking. Listing mencantumkan kisaran US$19–26/jam.
- Heritage Home Service mengiklankan Residential Permit Coordinator di Auburn, New Hampshire pada kisaran US$22–25/jam untuk tim residential HVAC/plumbing/electrical.
- John Owens Services di California muncul dalam listing Permit Coordinator/CSR sekitar US$24–30/jam.
- Puente Talent memiliki listing remote LATAM untuk Permit & Inspection Coordinator bagi klien HVAC di New England: US$1,200–1,700/bulan, full-time 9 AM–5 PM EST, termasuk permit, inspections, dan warranty registration. Kliennya tidak disebutkan secara publik.

Kesimpulan: perusahaan tertentu memang mengalokasikan budget untuk pekerjaan ini. Ini belum membuktikan bahwa mereka akan membeli layanan InstallFinish; itu masih harus diuji lewat pilot.

---

# 1. Naming

## Rekomendasi working name: InstallFinish Ops

**Alasan:**
- Tidak membatasi layanan hanya pada permit; bisa mencakup inspection, warranty, rebate, dan administrative closeout.
- Mudah dijelaskan ke owner HVAC.
- Nama menggambarkan hasil akhir: installation selesai secara operasional, bukan hanya teknisi selesai memasang unit.
- Bisa berkembang ke plumbing/electrical installation closeout tanpa rebrand besar.

**Tagline:** `Every install, properly closed out.`

**Positioning descriptor:** `Managed installation closeout for HVAC contractors.`

## Alternatif nama

| Nama | Kesan | Catatan |
|---|---|---|
| InstallFinish Ops | Jelas, praktis, service-led | Pilihan utama sementara |
| JobFinish Desk | Friendly, back-office service | Lebih luas dari HVAC |
| InstallFlow Ops | Workflow dan execution | “Flow” sangat umum; perlu clearance ketat |
| ReadyToClose Ops | Menekankan closeout | Agak panjang |
| FieldClose Desk | Field-service operations | Lebih abstrak untuk calon customer |

## Nama yang sebaiknya dihindari

- PermitFlow — sudah dipakai platform permitting besar.
- PermitOps — sudah dipakai beberapa pemain.
- PermitLane — sudah ada produk permit tracking.
- PermitPilot — dipakai beberapa produk/layanan permitting.
- PermitDesk — sudah dipakai di niche permitting.
- InstallDesk — sudah muncul sebagai nama/fitur CRM instalasi.
- Closeout Desk — sudah dipakai pemain closeout service.
- PermitRelay — sudah dipakai sebagai outbound channel di ekosistem PermitPipeline.
- TradeClose — domain premium sedang dipasarkan, jadi bukan pilihan hemat.

**Penting:** pencarian web bukan clearance hukum. Sebelum beli domain atau membuat logo, cek domain registrar, social handles, USPTO (dan negara tempat nanti beroperasi), serta kemiripan ejaan/pelafalan/kelas jasa. USPTO secara eksplisit menyarankan pencarian merek yang mirip bunyi, tampilan, makna, dan commercial impression — bukan exact match saja.

---

# 2. Product Requirements Document (PRD)

## 2.1 Problem statement

Residential HVAC contractors menjual dan memasang sistem heating/cooling, tetapi setiap instalasi dapat menghasilkan pekerjaan administrasi tambahan:
- menyiapkan dan mengirim dokumen permit;
- menunggu respons building department/municipality;
- meminta dokumen atau koreksi tambahan;
- menjadwalkan inspection bersama inspector, homeowner, dan install team;
- menangani failed inspection / reinspection;
- mendaftarkan equipment warranty;
- mengumpulkan dokumen closeout dan memperbarui status pekerjaan.

Pekerjaan tersebut tersebar di email, portal municipality, spreadsheet, CRM, foto teknisi, dan kalender. Jika tidak ada owner yang jelas, pekerjaan tertunda dan installation job belum benar-benar selesai secara administratif.

## 2.2 Target customer (ICP)

**Primary ICP:** independent/regional residential HVAC installation contractor di US yang:
- mempunyai installation team/field technicians;
- mengerjakan replacement/installation secara rutin;
- beroperasi di lebih dari satu municipality atau county;
- sudah punya office manager, dispatcher, install coordinator, atau sedang merekrut permit coordinator;
- memiliki backlog permit/inspection/warranty registration;
- bersedia memberi akses terbatas ke job documents dan SOP yang disetujui.

**Hipotesis kualifikasi awal:** 20+ installation jobs per bulan atau sekitar 5+ install crews. Angka ini adalah filter eksperimen, bukan benchmark industri yang telah terbukti.

**Buyer / champion:** Owner, COO, Installation Manager, Operations Manager, Office Manager, Permit Coordinator lead.

**Jangan target dulu:**
- bisnis dengan volume installation sangat kecil;
- perusahaan yang semua permit/inspection dikelola sempurna oleh tim internal;
- perusahaan yang mengharuskan kita mengambil keputusan legal/engineering atau menandatangani dokumen profesional;
- perusahaan tanpa process owner yang dapat approve dokumen dan memberi data.

## 2.3 Value proposition

`Your install team installs the system. InstallFinish handles the paperwork and follow-through that gets the job administratively closed.`

Customer membayar pekerjaan yang selesai, bukan seat, login, atau dashboard.

## 2.4 Service scope

### MVP — included

1. **Job intake**
   - Menerima job dari CSV/template, email, atau form sederhana.
   - Membuat case dan unique job reference.
   - Memeriksa field/dokumen wajib berdasarkan playbook customer yang telah disetujui.

2. **Permit packet preparation**
   - Mengumpulkan job scope, property address, homeowner/customer data, equipment model/serial, install date, contractor license details, dan supporting documents.
   - Menyusun draft package dan menandai data yang kurang.
   - Menggunakan checklist resmi/jurisdiction-specific yang diberikan atau telah diverifikasi serta disetujui customer.

3. **Human review & approval**
   - Customer/authorized contractor representative meninjau dan menyetujui dokumen.
   - Tidak ada submission atau pembayaran government fee tanpa approval eksplisit.

4. **Submission tracking**
   - Mencatat submission date, reference/permit number, authority, dan bukti pengiriman.
   - Mencatat status/response berdasarkan email, portal status, atau konfirmasi manusia.
   - Meneruskan request for information/corrections ke customer.

5. **Inspection coordination**
   - Mengumpulkan field-readiness confirmation dari installation team.
   - Menghubungi customer/authority melalui saluran yang diotorisasi untuk mengatur jadwal.
   - Mengonfirmasi waktu ke homeowner dan install team.
   - Mengelola reschedule/reinspection sebagai task baru.

6. **Warranty registration**
   - Mengumpulkan invoice, installation date, model/serial, contractor/dealer identifiers, dan customer data.
   - Menyiapkan/melakukan registration hanya berdasarkan authorization dan process yang disetujui.
   - Menyimpan confirmation number/certificate/bukti status.

7. **Closeout report**
   - Menampilkan outstanding items, deadlines, permit status, inspection status, warranty registration status, dan evidence.
   - Weekly exception report kepada customer.

### Phase 2 — setelah proses pertama terbukti

- Utility rebate paperwork dan incentive submission.
- Jobber / Housecall Pro / ServiceTitan integration.
- Manufacturer warranty claim administration.
- Multi-location and multi-jurisdiction operations.
- Customer portal terbatas untuk melihat case status dan approval.

### Out of scope

- Engineering, architecture, legal advice, code interpretation, or professional certification.
- Menjamin permit disetujui, inspection pass, atau rebate cair.
- Memalsukan, mengubah, atau mengasumsikan data yang tidak disediakan customer.
- Menandatangani dokumen atau memakai professional license customer.
- Autopilot municipal submissions tanpa review/authorization.
- Mengelola emergency HVAC dispatch / technical diagnosis.
- Menjadi CRM atau field-service management system.

## 2.5 Definitions of done / billable outcomes

| Outcome | Definition | Evidence |
|---|---|---|
| Permit package ready | Field dan attachment sesuai checklist versi yang disetujui | Checklist snapshot + files + reviewer approval |
| Permit submitted | Submission benar-benar dilakukan oleh authorized party | Portal receipt / email receipt / reference number |
| Inspection booked | Authority memberi slot dan pihak customer/crew sudah dikonfirmasi | Confirmation email / portal record / call note |
| Warranty registered | Pendaftaran diterima atau confirmation number diterbitkan | Manufacturer confirmation/certificate |
| Installation closeout complete | Semua item kontraktual untuk job tersebut selesai atau exception disetujui customer | Case closeout report + evidence list |

**Jangan bill sebagai “permit approved”** karena keputusan berada di tangan municipality. Jangan bill “inspection passed” kecuali kontrak secara eksplisit menjadikan itu outcome dan pembagian tanggung jawabnya disepakati.

## 2.6 User roles

- **InstallFinish Operator:** mengelola case, task, draft, dan komunikasi.
- **Quality Reviewer:** memeriksa packet dan bukti sebelum dikirim.
- **Customer Approver:** menyetujui submission, biaya, dan keputusan customer-side.
- **Customer Viewer:** melihat status, mengunduh laporan, tanpa bisa mengubah data kritis.
- **System Admin:** mengelola user, integrasi, permission, dan audit.

Satu orang boleh memegang lebih dari satu role di fase pilot, tetapi permission tetap dipisahkan.

## 2.7 Primary workflow

`RECEIVED → DATA_INCOMPLETE → READY_FOR_REVIEW → CUSTOMER_APPROVAL_PENDING → APPROVED_TO_SUBMIT → SUBMITTED → AUTHORITY_REVIEW → CORRECTION_REQUESTED (optional) → PERMIT_ISSUED / CLOSED_WITH_EXCEPTION → FIELD_READY → INSPECTION_SCHEDULED → INSPECTION_RESULT_RECORDED → WARRANTY_PENDING → WARRANTY_REGISTERED → CLOSED`

State tambahan:
- `NO_PERMIT_REQUIRED` hanya boleh dipilih berdasarkan customer-approved jurisdiction playbook / written confirmation, bukan tebakan AI.
- `ON_HOLD_CUSTOMER` jika informasi/approval belum diberikan.
- `CANCELLED` jika job batal.
- `INSPECTION_FAILED` membuat correction/reinspection task, bukan closeout.

## 2.8 Functional requirements

### P0 — MVP

- Create/update client account and job.
- Upload/download documents with access control.
- CSV import preview, row validation, duplicate detection, and commit.
- Municipality + job-type selection.
- Versioned checklist/playbook per client and jurisdiction.
- Case status/state transition with reason and evidence.
- Task assignment, due date, reminder, and escalation.
- Approval step before permit submission.
- Timeline/audit log for all meaningful actions.
- Warranty registration tracking.
- Inspection scheduling and outcome capture.
- Weekly report export (CSV/PDF or email summary).

### P1

- Email parsing and draft response suggestions.
- AI extraction from PDFs/photos/invoices/serial-number labels.
- AI summary of authority response email.
- Bulk case operations.
- Client read-only portal.

### P2

- CRM integrations and webhooks.
- Calendar integration.
- Jurisdiction playbook library with change review.
- Rebate workflows.

## 2.9 Non-functional requirements

- Tenant/client data isolation from day one.
- Every upload, approval, state change, submission, and outbound message audit logged.
- Idempotency for imports and webhook events.
- Retry with backoff for transient integrations; dead-letter/manual recovery view.
- Timezone-aware due dates and appointments; store timestamps in UTC and preserve authority timezone.
- Encryption in transit and at rest; signed, short-lived document links.
- Role-based access and least privilege.
- Data retention/deletion policy agreed in contract.
- No customer documents used for model training; configure provider privacy settings appropriate to client data.
- Human approval required for high-impact external actions.

## 2.10 MVP success metrics

Measure against each customer's baseline rather than inventing industry averages:
- Median time from job intake to complete permit packet.
- Percent of cases complete on first internal review.
- Time from field-ready to inspection booked.
- Number of overdue/stalled cases.
- Percent of applicable warranty registrations completed within the manufacturer's window.
- Admin hours spent by customer per case before vs after pilot.
- Cost per administratively closed installation.
- Customer renewal / repeat volume.

Pilot exit criterion: customer confirms the workflow removes real admin work and is willing to continue paying. A dashboard being used is not success by itself.

---

# 3. Technical implementation blueprint

## 3.1 Architecture principle

Build an internal operations console, not a public multi-tenant SaaS. The software is the delivery engine used by InstallFinish staff. The customer buys the managed result.

## 3.2 Suggested stack

| Layer | Choice | Why |
|---|---|---|
| Internal web app | Next.js + TypeScript | Fast operator UI |
| Backend | Java 21 + Spring Boot 3 | Strong workflow/domain model and integration handling |
| Database | PostgreSQL (Neon) | Relational cases, statuses, audit records |
| Document storage | Cloudflare R2 / S3-compatible | Large PDFs/photos; keep binaries out of DB |
| Automation | n8n (optional) | Notifications and low-risk connectors; not source of truth |
| AI | LLM API + OCR/document extraction | Extract and draft, never make authority decisions alone |
| Email | Dedicated mailbox/OAuth integration | Customer and authority communication trail |
| Calendar | Google/Microsoft calendar integration later | Inspection coordination |
| Monitoring | Sentry + structured logs | Detect failed workflows |

## 3.3 High-level components

1. **Intake API** — CSV/form/email ingestion, validation, dedupe.
2. **Case Management** — installation job, permit case, inspection, warranty.
3. **Playbook Service** — versioned customer-approved checklist and source references.
4. **Document Service** — upload, metadata, OCR extraction, secure retrieval.
5. **Task Engine** — owner, due date, reminders, escalation.
6. **Approval Engine** — reviewer/customer approvals for external actions.
7. **Communication Log** — inbound/outbound email/call notes and evidence.
8. **Outcome/Billing Ledger** — billable events derived from verified statuses.
9. **Reporting** — weekly exceptions, throughput, SLA performance.

## 3.4 Data model (initial)

All tenant-owned tables include `client_account_id`, `created_at`, `updated_at`, and soft-delete/archive policy where appropriate.

- `client_account`: legal/business name, timezone, operating area, contract tier, status.
- `client_user`: identity, role, account membership, active status.
- `service_location`: customer/job site address and normalized jurisdiction.
- `municipality`: authority name, state, portal URL, contact channel, timezone.
- `playbook`: client, municipality, job type, version, effective date, approved_by, source references, status.
- `playbook_checklist_item`: required data/document, owner, validation rule, source/evidence reference, ordering.
- `installation_job`: external job ID, work type, sold/installed dates, homeowner/customer, address, equipment details, contractor team, source system.
- `closeout_case`: job, lifecycle status, due dates, assigned operator, priority, case outcome.
- `permit_application`: permit type, jurisdiction, reference number, submitted date, authority status, receipt evidence.
- `inspection`: readiness date, requested slot, confirmed slot, result, failure reason, reinspection reference.
- `warranty_registration`: manufacturer, model/serial, deadline, submitted date, confirmation number, registration evidence.
- `rebate_case` (Phase 2): program, eligibility status, customer consent, submission and payout evidence.
- `document`: storage key, filename, mime type, checksum, category, access policy, extracted fields, source job/case.
- `task`: action, owner, due date, priority, status, escalation reason.
- `approval`: action type, approver, decision, decision timestamp, comment, immutable payload hash.
- `communication`: channel, direction, recipient, timestamp, summary, raw reference, consent/authorization basis.
- `billable_event`: event type, case, pricing rule version, evidence, approval, invoicing status.
- `audit_event`: actor, action, entity, before/after diff, timestamp, source IP/system.
- `integration_connection`: client, provider, credential reference (never store plaintext tokens), scopes, status.

### Key constraints

- Unique `client_account_id + external_job_id` where available.
- Dedupe key fallback from normalized site address + install date + serial/model + customer reference; show possible duplicates for human review.
- Equipment serial numbers are sensitive operational data; restrict access.
- Playbook version attached to each case so later checklist edits do not silently change historical jobs.
- Never mark `SUBMITTED`, `PERMIT_ISSUED`, `INSPECTION_PASSED`, or `WARRANTY_REGISTERED` without evidence.

## 3.5 API contract sketch

Base path: `/api/v1`

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/jobs/imports` | Upload CSV and create import batch |
| GET | `/jobs/imports/{batchId}/preview` | Return row validation and duplicate warnings |
| POST | `/jobs/imports/{batchId}/commit` | Commit validated jobs idempotently |
| GET | `/cases` | Search/filter closeout queue |
| GET | `/cases/{caseId}` | Case details, docs, timeline |
| POST | `/cases/{caseId}/documents` | Upload document metadata and storage object |
| POST | `/cases/{caseId}/checklist/validate` | Validate against selected playbook version |
| POST | `/cases/{caseId}/approval-requests` | Request customer approval |
| POST | `/cases/{caseId}/approvals` | Record approval/decline |
| POST | `/cases/{caseId}/permit-submissions` | Record a real submission with proof |
| POST | `/cases/{caseId}/authority-events` | Record authority response/request |
| POST | `/cases/{caseId}/inspections` | Create inspection coordination task |
| PATCH | `/inspections/{inspectionId}` | Record confirmed schedule/result |
| POST | `/cases/{caseId}/warranty-registrations` | Record warranty workflow and proof |
| GET | `/reports/weekly?from=&to=` | Weekly throughput/exceptions report |
| GET | `/billable-events` | Verified billable units for review |

All write endpoints should support idempotency keys for retryable requests. Validate webhook signatures. Use optimistic locking/version field for case status changes.

## 3.6 AI boundaries

**Allowed AI jobs:**
- Extract text/fields from invoices, equipment labels, PDFs, and photos.
- Suggest classification of documents.
- Compare extracted fields with a human-authored checklist.
- Flag missing or conflicting data.
- Summarize an authority email and identify requested next action.
- Draft an email/call checklist for human approval.

**Not allowed without human/certified customer decision:**
- Determine which permits are legally required from scratch.
- Interpret building/mechanical/electrical code.
- Decide that a project is exempt from a permit.
- Submit or certify applications, pay fees, or sign documents automatically.
- Claim authority approval based on model inference.
- Tell technicians how to perform HVAC work.

Every AI suggestion stores model/provider, prompt version, input references, output, reviewer, and final human disposition. AI output must be treated as untrusted draft data.

## 3.7 Integrations strategy

**Pilot:** CSV + secure document upload + email. Do not build 3 CRM integrations before one paying customer.

**Next:** integrate the actual source system of the first customer. Jobber has developer APIs and webhooks. Housecall Pro API access may depend on the customer plan (official docs indicate API access on MAX). ServiceTitan has developer/API ecosystem but can involve additional onboarding and customer permissions. Confirm each provider's current access and contract requirements before promising an integration.

**Do not** use browser automation to bypass portals, shared credentials, CAPTCHA, or access restrictions. Use documented APIs/authorized accounts/manual human operation according to the authority's rules.

## 3.8 Security and compliance controls

- Sign a services agreement and data processing terms with each customer.
- Document controller/processor roles and approved subprocessors.
- Keep customer-provided contractor license and municipal account access under customer control.
- Request delegated named users where a portal supports it; do not collect shared passwords unless explicitly approved and safely managed.
- Use least-privilege users and MFA.
- Minimize homeowner PII and retain only what the workflow requires.
- Encrypt files and database connections; use short-lived signed file URLs.
- Record authorization and consent for customer communication.
- Do not send marketing texts to homeowners as part of this service. Appointment/admin messages must be within the customer's permitted relationship and consent basis.
- Provide correction/deletion process and data export on termination.
- Obtain US privacy/legal review before processing at scale; state privacy laws and customer contracts may add obligations.

## 3.9 Minimal UI

1. **Operations Queue:** New / missing data / approval pending / authority waiting / inspection due / warranty due.
2. **Case Detail:** job summary, checklist, docs, status, tasks, timeline, approval controls.
3. **Playbooks:** municipality + job type + version + sources + approver.
4. **Tasks:** operator tasks, due dates, reminders, escalations.
5. **Reports:** weekly job counts, aging, missing data, inspection pipeline, warranty completion, customer time saved.
6. **Settings:** users, roles, client configuration, integrations.

Don't spend the first sprint on charts. Queue, case detail, and evidence log matter most.

## 3.10 Build sequence

### Sprint 0 — Sell/operate manually (parallel to coding)
- Create intake CSV template.
- Create one sample playbook from the pilot customer’s official process.
- Run 2–3 sample cases manually with customer-provided/fictional data.
- Confirm approval, evidence, and handoff model.

### Sprint 1 — Internal MVP
- Backend schema + migrations.
- Case intake and CSV preview/commit.
- Case queue and detail screen.
- File upload + metadata.
- Playbook checklist validation.
- Status transitions + audit events.
- Task due dates + weekly report.

### Sprint 2 — Assisted processing
- OCR extraction.
- Draft missing-doc emails.
- Approval workflow.
- Warranty-registration tracking.
- Inspection scheduling log.
- Error/retry monitoring.

### Sprint 3 — Customer-specific productionization
- First real CRM/email/calendar integration.
- Client viewer / report access.
- Billing ledger and contract capacity.
- Runbook for operator and QA.

**MVP exit:** one paying pilot customer can send a job batch, we can operate it safely, and we can produce auditable closeout results. A public sign-up / subscription system is explicitly not required.

---

# 4. Offer, pricing, and marketing

## 4.1 Initial commercial offer

**Offer name:** `10-Job Installation Closeout Pilot`

**Promise:** We take a batch of 5–10 HVAC installations through the agreed administrative closeout workflow: document completeness, permit packet preparation/tracking, inspection coordination, and warranty registration where applicable.

**Pilot scope:**
- one contractor;
- one or two municipalities/jurisdictions;
- one installation type / equipment family;
- 5–10 live or recently completed jobs;
- 2–3 weeks depending on authority response (government wait time is outside our control);
- customer approves/signs all submissions and fees;
- weekly exception report.

**Pilot deliverables:** case tracker, missing-item list, permit status evidence, confirmed inspection calendar entries where possible, warranty confirmation evidence, and before/after admin effort estimate.

## 4.2 Pricing hypothesis (not established market pricing)

Public job listings show examples of US$19–30/hour full-time coordinator roles and one remote LATAM HVAC coordinator listing at US$1,200–1,700/month. Those are hiring signals, not market-rate benchmarks or proof of willingness to pay InstallFinish.

Test three models in discovery; don't publish a price until the work is timed:

1. **Paid pilot:** fixed fee for a small number of cases (e.g., US$400–700 for 5–10 jobs) as a pricing hypothesis.
2. **Monthly managed desk:** base fee covering agreed capacity + overage per case.
3. **Per-closeout:** price per agreed administrative unit, with separate pricing for corrections/reinspection or unusual jurisdictions.

Track actual operator minutes, number of authority interactions, missing documents, and complexity per job. The price must cover human delivery, quality review, software, payment fees, and margin. Avoid pure success fee on government approvals because InstallFinish does not control approval decisions.

## 4.3 Positioning

**Headline:** `Your HVAC install team shouldn't spend its day chasing permits and inspections.`

**Subheadline:** `InstallFinish runs the paperwork, status follow-ups, inspection coordination, and warranty registration behind your installations — while your team keeps control of technical decisions and approvals.`

**Three bullets:**
- One accountable desk for every active installation case.
- Missing documents and stalled cases surfaced before they block closeout.
- Every completed step backed by a receipt, confirmation, or audit trail.

**CTA:** `Request a 10-job closeout pilot`.

## 4.4 Sales motion

1. Find companies currently hiring Permit Coordinator, Installation Coordinator, Permit Processor, Inspection Coordinator, Warranty Coordinator, or Rebate Coordinator.
2. Verify the company’s residential HVAC installation services and service areas.
3. Identify the owner/operations/install manager, not a homeowner service line as first choice.
4. Send one personalized email referencing the exact open role or workflow. Do not imply knowledge of their backlog beyond their public job description.
5. Follow up once with a one-page sample workflow or checklist.
6. Hold a 15-minute discovery call.
7. Offer a paid 5–10 job pilot with clear data-access and approval boundaries.

**Strong opening angle:** “I saw your team is hiring for a permit/inspection coordinator. Would it be useful to test a small managed overflow desk on 5–10 installations while you recruit or during peak volume?”

## 4.5 Channels

- Founder-led personalized email to companies with active job postings.
- Direct business calls to ask for the installation/operations manager; no robocalling.
- LinkedIn to owners and operations/install managers with a relevant one-to-one note.
- ACCA/TACCA and state/local contractor directories.
- HVAC distributor / manufacturer rep relationships.
- Recruitment firms placing remote HVAC operations coordinators (possible referral/white-label partners).
- Educational content: `HVAC installation closeout checklist`, `permit-to-inspection tracker`, `warranty registration deadline checklist`.

## 4.6 Marketing content

Create three assets before running ads:

1. **One-page PDF:** Installation closeout checklist with fields and evidence, not generic AI marketing.
2. **Demo case:** fictional/redacted job showing intake → missing document → approval → submitted → inspection scheduled → warranty registered. Clearly label it as a demo, not a customer case study.
3. **Landing page:** headline, target customer, scope, process, security/approval boundaries, pilot CTA.

Content angles:
- “What happens after the HVAC install is finished?”
- “Five admin steps that keep a sold installation from being fully closed out.”
- “Why permit submission and inspection scheduling need separate owners.”
- “Warranty registration windows: what your office needs before the job closes.”

No invented ROI or case-study claims. Once there is a pilot, publish only approved, anonymized metrics.

## 4.7 Lead scoring

Score leads 0–100:

- +30 active permit/inspection coordinator job ad.
- +20 explicit residential HVAC installation service.
- +15 multi-location / multiple service areas.
- +15 public evidence of install volume or multiple install crews.
- +10 more than one trade (HVAC/plumbing/electrical), suggesting a repeatable admin desk.
- +10 reachable owner/operations leader.

Subtract:
- −20 commercial mega-project focus requiring engineering/code expertise.
- −15 no evidence of installations (service/repair only).
- −15 no apparent process owner / no response channel.

Prioritize leads scoring 60+. This is our internal prospecting heuristic, not a validated predictive model.

---

# 5. Initial lead list (signals to verify before contact)

These are **prospecting leads**, not confirmed customers. The reason they are listed is the public hiring signal for the exact work we want to perform. Re-check that each job is still open and confirm the correct business entity/contact before outreach.

| Priority | Company | Location | Public demand signal | Best first approach |
|---|---|---|---|---|
| A | Air Around the Clock | Deerfield Beach, Florida | Permit Coordinator listing posted Oct 26, 2026; role describes around 10 permits/day and inspection follow-through | Contact owner/operations manager; offer overflow pilot on 5–10 installs while recruiting |
| A | Must Air USA | Hallandale Beach, Florida | HVAC Permit Processor & Purchasing Coordinator; permits, inspections, warranty processing, equipment purchasing; listing shows US$19–26/hr | Pitch combined permit + warranty closeout, not generic admin staffing |
| A | Ace Air Conditioning, Inc. | DeBary, Florida | Permit & Licensing Coordinator listing includes HVAC/electrical permits, inspections, license records, equipment registration | Contact via official company site/office; propose one-job-type pilot |
| A | John Owens Services | Petaluma / Marin & Sonoma, California | Permit Coordinator / CSR listing; official website provides contact page, offices and phone numbers | Ask for operations/installation manager; pitch permits + inspection + closeout overflow |
| B | Heritage Home Service | Auburn, New Hampshire | Residential Permit Coordinator role for residential HVAC/plumbing/electrical | Approach regional operations leadership; larger multi-trade potential |
| B | Mandolin HVAC and Construction LLC | Raleigh, North Carolina | Permit/Inspections Coordinator listing at US$21–23/hr; job includes municipal paperwork and inspection scheduling | Verify entity and contact details before outreach |
| Partner | Puente Talent | Remote placement for a New England HVAC contractor | Public role for remote LATAM Permit & Inspection Coordinator at US$1,200–1,700/month, including permits, inspections and warranty registration | Explore referral/white-label conversation; the end client is not publicly named |

### Public sources / contact routes

- Air Around the Clock job listing: https://www.jobtarget.com/jobs/jt-r38f0h2amz/project-coordinator-deerfield-beach-florida
- Florida license record for Air Around the Clock: https://www.myfloridalicense.com/portalsearches/VerifyLicensee/LicenseDetail?ID=6a073e381ca35b690a66fc4c4e115cfb
- Air Around the Clock BBB profile / public phone: https://www.bbb.org/us/fl/deerfield-beach/profile/air-conditioning-contractor/around-the-clock-ac-service-llc-0633-4004806
- Must Air USA listing: https://www.wayup.com/i-j-HVAC-Permit-Processor-Purchasing-Coordinator-Must-Air-USA-273095649556011/
- Ace Air job listing: https://www.career.com/job/ace-air-conditioning-inc/permit-licensing-coordinator/j202607191013532053931
- Ace Air official site: http://www.aceac.com
- John Owens Services careers signal: https://hiringcafe.com/jobs/permit-services-support-coordinator
- John Owens official contact page: https://johnowensservices.com/contact-us
- Heritage Home Service role: https://www.linkedin.com/jobs/view/residential-permit-coordinator-at-heritage-home-service-4460206722
- Mandolin HVAC job listing: https://www.snagajob.com/jobs/1305612504
- Puente Talent remote role: https://puentetalent.com/jobs/permit-inspection-coordinator-2604

### Repeatable lead sourcing

Weekly process:
1. Search job boards for exact phrases: `HVAC permit coordinator`, `installation permit coordinator`, `permit processor HVAC`, `inspection coordinator HVAC`, `warranty registration HVAC`, `install coordinator permits`.
2. Search 3–5 states/metros, starting where the job ads are currently appearing (Florida, New Hampshire/Massachusetts, California, North Carolina).
3. Verify company has residential installation, not just commercial engineering or repair-only work.
4. Find official site, business phone, and operations/owner name from official company sources.
5. Put each prospect into a simple CRM with source URL, job-post date, last checked date, signal, contact route, score, and next action.
6. Re-check job status before sending any personalized reference to the listing.

Useful directories:
- ACCA contractor locator: https://hvac-contractors.acca.org/
- TACCA membership directory: https://www.tacca.org/
- State contractor license registries (Florida DBPR / relevant state authority).
- Local supply-house and HVAC distributor partner directories.

Do not scrape sites against their terms, buy questionable consumer contact lists, or message homeowners. Only prospect the business decision-makers using appropriate business contact channels.

---

# 6. Outreach assets

## First email — hiring signal

**Subject:** Permit & inspection overflow for {{Company}}

Hi {{FirstName}},

I came across your opening for a {{RoleTitle}} and noticed the role covers permit paperwork and inspection coordination for your HVAC installations.

We’re building a small managed closeout desk for residential HVAC contractors. We take a defined batch of installation jobs and handle the admin workflow: document checks, permit packet preparation, status follow-ups, inspection scheduling, and warranty registration where applicable.

Your team keeps control of code decisions, approvals, municipal accounts, and any required signatures. We provide a case-by-case audit trail so nothing gets marked complete without evidence.

Would it be useful to test this on 5–10 installations as an overflow pilot while you recruit or during a busy period?

Best,  
{{YourName}}  
InstallFinish Ops  
{{Business address}}  
{{Website}}  

If this is not relevant, reply “no thanks” and I won’t follow up.

## Follow-up (one time)

**Subject:** Re: Permit & inspection overflow for {{Company}}

Hi {{FirstName}},

Just following up once. The pilot is intentionally small: one install type, one or two municipalities, and 5–10 jobs. We would first map your existing checklist and approval process, then report the time spent, missing items, and closeout status for each case.

If another person owns installation permitting, could you point me to the right role?

Best,  
{{YourName}}

## Discovery call questions

1. Roughly how many residential installs need permit/inspection admin each month?
2. Which municipalities/cities generate the most paperwork?
3. Who currently prepares and submits permit applications?
4. Who follows up when a municipality requests more information?
5. Who books inspections and handles failed inspections?
6. Is warranty registration handled per job, and how do you confirm completion?
7. Which system stores job details today (ServiceTitan, Jobber, Housecall Pro, spreadsheet, etc.)?
8. What is the current backlog / average time from install completion to administrative closeout?
9. Which actions must be performed by a licensed employee or an authorized account holder?
10. Would you pay for a small pilot if the scope, approval boundaries, and evidence requirements are agreed in writing?

---

# 7. 30-day go-to-market execution

## Week 1 — customer discovery + concierge pilot setup

- Create a one-page offer, sample closeout case, and simple intake CSV.
- Build a list of 30 companies with current hiring signals; do not mass blast.
- Send 10–15 carefully personalized emails and make 5–10 manual business calls during target business hours.
- Aim for 3–5 discovery conversations, not a vanity number of emails.
- Ask for a redacted sample workflow/doc set under NDA/service agreement before touching real PII.

## Week 2 — close one paid pilot

- Choose one contractor, one installation type, and one or two municipalities.
- Time every task; record missing data and every human interaction.
- Agree on role boundaries, sign-off, access, government fees, and data handling.
- Run 5–10 jobs manually with a spreadsheet + secure folder if necessary.

## Week 3 — internal tooling

- Only automate the slowest repeated tasks observed during pilot.
- Add case queue, documents, checklist version, approvals, timeline, and weekly report.
- Do not build public self-serve account registration or subscription billing.

## Week 4 — deliver, measure, renew

- Compare baseline admin effort with actual effort.
- Review incomplete submissions, stalled cases, inspection response time, warranty registration status.
- Ask whether customer wants another 10–20 jobs or monthly capacity.
- Get explicit permission before publishing anonymized results/testimonial.

### Weekly operating targets (internal hypotheses)

- 30 new researched accounts.
- 10–15 personalized first-touch emails.
- 5–10 business calls or LinkedIn conversations.
- 3 discovery calls.
- 1 pilot proposal.

These are activity targets for disciplined founder-led sales, not forecast conversion rates.

---

# 8. Risk register and safeguards

| Risk | Why it matters | Control |
|---|---|---|
| Local permit variation | Municipality rules differ and change | Customer-approved playbook with source URL/version/review date; human confirmation |
| Unauthorized professional judgement | Permit applicability/code can require licensed expertise | No code/legal/engineering advice; contractor approves and signs |
| Government response delays | We do not control agency SLA | Sell administrative deliverables, not approval speed guarantees |
| Customer data exposure | Homeowner addresses, invoices, serials, contact data | Least privilege, encryption, retention policy, DPA, audit logs |
| Missed inspection | Can delay payment/commissioning and damage trust | Dual confirmation, timezone checks, reminders, human escalation |
| Working hours | US municipal offices work US business hours; Jakarta is ~12–13 hours ahead | Start with async document preparation; hire/partner with US/LATAM operator for calls if volume requires |
| Human delivery margin | RaaS is not fully automated | Time tracking per case, SOPs, automation only for repeated steps |
| Name/trademark collision | Similar names already exist | Treat InstallFinish as provisional; run legal/domain clearance before launch |

---

# 9. Decision gates — don't build blindly

Proceed to full automation only if these three conditions are met:

1. **Demand gate:** At least two HVAC contractors confirm that permit/inspection/warranty admin is a meaningful recurring bottleneck, and one agrees to a paid pilot.
2. **Delivery gate:** We can run 5–10 cases safely with customer-approved process docs and no unauthorized decisions.
3. **Unit economics gate:** We know real operator time and customer value per case well enough to price for positive gross margin.

If no one pays for the pilot after 30–40 well-qualified, personalized approaches, revisit the niche and offer before writing more software.

---

# 10. Research sources

## Demand and hiring signals

- Air Around the Clock Permit Coordinator listing (posted Oct 26, 2026): https://www.jobtarget.com/jobs/jt-r38f0h2amz/project-coordinator-deerfield-beach-florida
- Mandolin HVAC & Construction Permit/Inspections Coordinator: https://www.snagajob.com/jobs/1305612504
- Must Air USA HVAC Permit Processor & Purchasing Coordinator: https://www.wayup.com/i-j-HVAC-Permit-Processor-Purchasing-Coordinator-Must-Air-USA-273095649556011/
- Ace Air Conditioning Permit & Licensing Coordinator: https://www.career.com/job/ace-air-conditioning-inc/permit-licensing-coordinator/j202607191013532053931
- Heritage Home Service Residential Permit Coordinator: https://www.linkedin.com/jobs/view/residential-permit-coordinator-at-heritage-home-service-4460206722
- Puente Talent Permit & Inspection Coordinator (HVAC client, remote LATAM): https://puentetalent.com/jobs/permit-inspection-coordinator-2604
- John Owens Services contact: https://johnowensservices.com/contact-us

## Competitor landscape

- PermitFlow company overview and claims: https://www.permitflow.com/about-us
- PermitOps coordination packets: https://permitops.io/
- PermitLane (Canadian permit tracking): https://permitlane.ca/
- PermitPilot outsourced contractor permitting: https://usepermitpilot.com/
- CallbackOps: https://callbackops.com/
- FieldGain: https://fieldgain.co/
- Jobber Receptionist: https://www.getjobber.com/features/ai-receptionist/
- Closeout Control: https://closeoutcontrol.com/

## Manufacturer / rebate workflow evidence

- LG HVAC registration page (including bulk registration template and registration time-window details): https://lg-dfs-warranty.com/register
- ENERGY STAR Service Provider Partner material describing Sealed Pro qualification/calculation/processing: https://www.energystar.gov/sites/default/files/2024-10/Leveraging%20SPP%20508C.pdf

## Compliance and technical references

- USPTO trademark search: https://www.uspto.gov/trademarks/search
- USPTO federal trademark searching guidance: https://www.uspto.gov/trademarks/search/federal-trademark-searching
- FTC CAN-SPAM compliance guide: https://www.ftc.gov/business-guidance/resources/can-spam-act-compliance-guide-business
- Twilio A2P 10DLC overview (only relevant if SMS is later added): https://www.twilio.com/docs/messaging/compliance/a2p-10dlc
- Jobber developer docs: https://developer.getjobber.com/docs/
- Housecall Pro API docs: https://docs.housecallpro.com/
- ServiceTitan developer portal: https://developer.servicetitan.io/

---

# Final recommendation

**Do not start by building a SaaS product.** Start by selling a small, highly-defined managed service: HVAC installation closeout for 5–10 jobs. Use AI to extract and check documents, but keep customer approvals and authority-facing decisions under human control.

The first proof is not a beautiful dashboard. The first proof is a contractor handing over real installation cases, paying for the pilot, and asking us to take the next batch.
