# Provedores de IA

| Tipo | Endpoint | Autenticação | Observações |
|---|---|---|---|
| OpenAI e compatíveis | `{base}/chat/completions`, `{base}/models` | `Authorization: Bearer` | `response_format: json_object` quando JSON é pedido; base padrão `https://api.openai.com/v1` |
| Anthropic | `{base}/v1/messages`, `/v1/models` | `x-api-key`, `anthropic-version: 2023-06-01` | Sem modo JSON nativo: o prompt exige JSON e o parser valida |
| Gemini | `{base}/v1beta/models/{modelo}:generateContent`, `/v1beta/models` | `x-goog-api-key` | `responseMimeType: application/json` |

**Validação:** formatos escritos de acordo com a API pública conhecida e testados contra servidor simulado. **Nenhuma chamada real foi feita**; confirme nomes de modelos, versões e campos na documentação vigente de cada provedor. Modelos e suporte a imagem/ferramentas/streaming variam: o app não assume nada (`capabilities()` é conservador) e o teste de conexão faz 3 etapas reais: listar modelos (credencial), gerar texto, devolver um plano JSON válido.

Chave: digitada no campo, gravada cifrada (AES-GCM, chave no Android Keystore), nunca exibida de volta, nunca em logs/erros (`Redactor` + mensagens sem corpo bruto). Retentativas com backoff para 429/5xx/rede, respeitando `Retry-After`; 401/403 não são repetidos. Fallback de modelo opcional.

Não implementados: teste de visão, streaming (a interface existe, a implementação emite um bloco), estimativa de custo, modelos locais.
