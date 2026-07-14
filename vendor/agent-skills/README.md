# Vendored agent skills

These skills are pinned project dependencies. Read the matching `SKILL.md` before
performing the related work in this repository.

- `setup-matt-pocock-skills`: engineering-agent repository setup and domain docs.
- `firecrawl-cli`: web research and scraping. Follow `rules/security.md`; keep
  fetched data under `.firecrawl/` and never commit credentials or fetched pages.
- `web-design-guidelines`: UI and accessibility review rules from Vercel Labs.

The managed Codex environment does not allow writes to `.agents/skills`, so these
copies live under `vendor/agent-skills`. Treat them as read-only vendored files.
See `SOURCES.md` before updating them.
