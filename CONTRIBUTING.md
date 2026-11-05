# Contributing

## Branch model
| Branch      | Purpose                                                    |
|-------------|------------------------------------------------------------|
| `main`      | Production. Only receives release PRs from `hmg`.          |
| `hmg`       | Homologation (integration branch). Receives feature PRs.   |
| `feat/*`    | One branch per feature, created from `hmg`.                |
| `fix/*`     | Bug fix branches, created from `hmg`.                      |

Releases are tagged on `main` as `vMAJOR.MINOR.PATCH`.

## Workflow
1. `git checkout hmg && git pull`
2. `git checkout -b feat/<short-name>`
3. Work in small, logical commits, writing the test first (TDD).
4. Make sure `./mvnw verify` is green (integration tests need Docker).
5. Push the branch and open a Pull Request into `hmg` using the PR template.
6. CI must pass. Merge with a merge commit and delete the branch.
7. To release, open a PR `hmg` -> `main` ("Release vX.Y.Z"), merge it, then tag `main`.

## Commit convention
Commit messages are in English, in the imperative mood, and use one of two prefixes:

- `feat: add stock movement ledger` - new behaviour or improvement
- `fix: reject negative adjustments` - bug fix

## Architecture rules
- `core` must not depend on Spring (only `jakarta.persistence` / `jakarta.validation`).
- `application` depends on `core`; `infrastructure` holds all framework code.
- Controllers depend on use-case interfaces only.
- Every entity change needs a new Flyway migration (`V<n>__description.sql`); never edit an existing one.
