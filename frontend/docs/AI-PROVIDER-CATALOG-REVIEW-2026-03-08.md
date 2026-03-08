# AI Provider Catalog Review - 2026-03-08

## Scope analyzed
- `catalogo_completo_apis_ia_mar_2026.md`
- `Faca um Busca avassaladora e traga o arquivo . MD.md`
- `catalogo_apis_ia_gratuitas_e_pagas_mar_2026.md`
- `quero uma lista agora das pagas, ordene das mais b.md`
- `Faca um Double Chexk ainda mais rigoroso!.md`
- `Procure todas os servicos de IA gratuitos que tem.md`
- `Solucoes de API para Busca na Dark Web e Deep Web.md`

## Reliability assessment
- The markdown set is useful as market reconnaissance, not as a source of truth.
- Several files mix official docs, marketing pages, third-party blogs and pricing roundup pages.
- Counts, tiers and free-credit claims are inconsistent across files.
- Multiple statements are already framed as uncertain or "double-check required" inside the source material itself.

## High-confidence conclusions
- The repo currently had only a small static chat model catalog.
- The product needed a normalized provider registry and explicit runtime configuration contract.
- The source material consistently points to a real product need: multi-provider AI gateway, catalog visibility, agent/version visibility and API-key onboarding.

## Contradictions found
- OpenRouter free model counts vary substantially between files.
- OpenAI free credits are described both as present and discontinued.
- xAI and other newer providers are described with uncertain trials and non-primary pricing references.
- Some pricing rankings are derived from non-official aggregators and should not drive billing logic.

## Implementation decisions taken
- A normalized provider registry was added for mainstream AI APIs referenced repeatedly across the markdown corpus.
- Agent catalog and agent versions were formalized in-code.
- A unified `/api/v1/ai/chat` route now resolves models, agents and fallbacks.
- Provider discovery endpoints and API-key requirement endpoints were added.
- Runtime configuration currently relies on environment variables, which is safer than storing raw secrets in generic workspace JSON.

## Explicitly excluded from implementation
- Dark web / deep web monitoring and threat-intel APIs were not integrated into the product runtime.
- Reason: they materially increase abuse, legal and operational risk and are outside the safe default scope of a general AI workspace product.
- The dark web markdown was analyzed as research input only, not as an implementation target.

## Provider groups normalized into the solution
- LLM and chat: OpenAI, Anthropic, Google Gemini, DeepSeek, Groq, Mistral, Cohere, Perplexity, OpenRouter, Together AI, Fireworks AI, xAI, Cerebras, SambaNova, NVIDIA NIM, DeepInfra, SiliconFlow.
- Enterprise or platform variants: Azure OpenAI, AWS Bedrock, GitHub Models, Cloudflare Workers AI, Hugging Face, AI21.
- Search and research: Exa, NewsCatcher.
- Image and creative: Stability AI, fal.ai, Replicate, Runway.
- Speech and audio: Deepgram, AssemblyAI, ElevenLabs.

## Notes for future hardening
- Move provider secrets to a dedicated encrypted credential store instead of generic config storage.
- Add provider-specific health checks with real network probes only in secure environments.
- Add modality-specific routes for image, speech, search and transcription instead of funneling everything through chat.
- Re-verify pricing, quotas and free tiers only against official provider sources before any commercial claim is shown in the UI.
