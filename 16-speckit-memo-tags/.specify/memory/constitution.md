# Memo Service Constitution

## Core Principles

### I. Test-First (NON-NEGOTIABLE)
Every behavior change MUST start with a failing automated test. The order is: write the test, see it fail
for the expected reason, implement, see it pass. A task that adds production code without a prior failing
test MUST NOT be marked complete. Rationale: the spec's acceptance scenarios are only trustworthy if they
are executable.

### II. Backward-Compatible API
Existing endpoints, request bodies, and response fields MUST keep working for existing clients. New fields
and parameters MUST be optional. A breaking change requires an explicit MAJOR version note in the spec.
Rationale: the React client and any scripts already depend on the current contract.

### III. Simplicity (YAGNI)
Implement only what the current spec requires. No new infrastructure (cache, queue, search engine) and no
new runtime dependency unless the plan justifies it in the Complexity Tracking table. Rationale: this is a
small service; every added moving part must pay for itself.

### IV. Clear Errors
Invalid input MUST return a 4xx status with a machine-readable error body that names the offending field.
Silent truncation or silent dropping of invalid input is forbidden. Rationale: callers can only fix what
they can see.

### V. Reproducible Environment
The feature MUST run and be verifiable with `docker compose up` and `docker run` of the build image alone.
No step may require software installed on the developer machine beyond Docker. Rationale: results must be
reproducible by anyone who clones the repository.

## Technology Constraints

- Java 17, Spring Boot 3.3.x, Spring Data JPA, PostgreSQL 16 (production), H2 (tests only).
- Schema changes are applied by JPA (`ddl-auto: update`) in this project; destructive changes (dropping or
  renaming columns) are out of scope without a separate migration plan.

## Development Workflow

- Order of work: constitution -> specify -> clarify -> plan -> tasks -> analyze -> implement -> converge.
- No production code is written before the spec is reviewed and the plan passes the Constitution Check.
- Each task is committed separately and references its task ID.

## Governance

This constitution supersedes ad-hoc practices. Amendments require a spec-level note and a version bump:
MAJOR for removing or redefining a principle, MINOR for adding a principle or materially expanding
guidance, PATCH for wording. Every plan MUST include a Constitution Check against these principles, and
violations MUST be justified in writing.

**Version**: 1.0.0 | **Ratified**: 2026-11-13 | **Last Amended**: 2026-11-13
