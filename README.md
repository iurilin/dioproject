# API de Orçamento com Assistente de Voz

API REST em Spring Boot que controla receitas e despesas e entende comandos em linguagem natural, inclusive por **áudio**. A pessoa envia um arquivo de voz, a aplicação o transcreve, usa IA para entender a intenção e executa uma função real (registrar ou consultar transações).

> Exemplo: o áudio *"gastei trinta reais de Uber hoje"* vira uma despesa de R$ 30,00 na categoria `TRANSPORTE`, gravada com a data de hoje.

## Como funciona

```
áudio ──► Whisper (transcrição) ──► texto ──► IA extrai a intenção e os dados
                                                        │
                                                        ▼
        resposta em texto ◄── TransacaoService ◄── código da aplicação executa
                                   │
                                   ▼
                              banco de dados
```

A IA **não executa nada e não calcula nada**. Ela só traduz a frase para dados estruturados (intenção, valor, categoria, período). Quem grava, valida e soma é o código da aplicação, então as mesmas regras valem para a API REST e para a voz, e os números sempre vêm do banco.

### Intenções reconhecidas

| Intenção | Exemplo de frase |
|---|---|
| Registrar | "gastei 45 reais no mercado ontem", "recebi 3000 de salário" |
| Total por categoria | "quanto gastei com mercado esse mês" |
| Saldo | "qual meu saldo" |
| Listar | "lista minhas transações de setembro" |
| Outro | qualquer outra pergunta cai em uma conversa normal |

Os períodos reconhecidos são hoje, ontem, esta semana, este mês, mês passado, este ano ou um intervalo de datas. O cálculo das datas é feito pelo código (`PeriodoResolver`), não pela IA, porque modelos erram conta de calendário.

## Tecnologias

- Java 21, Spring Boot 4
- Spring AI (cliente OpenAI-compatível)
- [Groq](https://groq.com) como provedor de IA, no plano gratuito: modelo `openai/gpt-oss-120b` para o texto e `whisper-large-v3-turbo` para a transcrição
- Spring Data JPA com H2 (em memória)
- Bean Validation
- JUnit 5, Mockito e AssertJ nos testes
- Gradle

## Estrutura

```
com.dio.project
├── config        configuração do ChatClient e do prompt de sistema
├── controller    endpoints REST (sem regra de negócio)
├── domain        entidade Transacao e enums (Categoria, TipoTransacao)
├── dto           requests/responses da API e comandos extraídos pela IA
├── exception     exceção de negócio e tratamento global de erros
├── repository    acesso a dados (Spring Data JPA)
└── service       regras de negócio, transcrição e orquestração do assistente
```

O prompt de sistema fica em `src/main/resources/prompts/system.st`.

## Como rodar

### Pré-requisitos

- JDK 21
- Uma chave de API da Groq (gratuita, sem cartão): crie em [console.groq.com](https://console.groq.com)

### Configuração

A chave **nunca** deve ser escrita em arquivos do repositório. Defina a variável de ambiente `GROQ_API_KEY`:

```bash
# Windows (feche e reabra o terminal/IDE depois)
setx GROQ_API_KEY "gsk_sua-chave-aqui"
```

Ou, no IntelliJ, em *Run > Edit Configurations > Environment variables*.

O `application.properties` lê a chave da variável:

```properties
spring.ai.openai.base-url=https://api.groq.com/openai/v1
spring.ai.openai.api-key=${GROQ_API_KEY}
spring.ai.openai.chat.options.model=openai/gpt-oss-120b
spring.ai.openai.audio.transcription.options.model=whisper-large-v3-turbo
spring.ai.openai.audio.transcription.options.language=pt
```

### Executar

```bash
./gradlew bootRun
```

A API sobe em `http://localhost:8080`. O banco H2 é em memória: **os dados são apagados a cada reinício**. O console do banco fica em `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:orcamento`).

## Endpoints

### Transações (REST)

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/transacoes` | Cria uma transação |
| GET | `/api/transacoes?inicio=&fim=&categoria=` | Lista por período (categoria é opcional) |
| GET | `/api/transacoes/total?categoria=&tipo=&inicio=&fim=` | Total por categoria (`tipo` padrão: `DESPESA`) |
| GET | `/api/transacoes/saldo?inicio=&fim=` | Saldo (receitas menos despesas) |
| DELETE | `/api/transacoes/{id}` | Remove uma transação |

Datas no formato `yyyy-MM-dd`.

**Categorias:** `ALIMENTACAO`, `MERCADO`, `TRANSPORTE`, `MORADIA`, `SAUDE`, `EDUCACAO`, `LAZER`, `SALARIO`, `OUTROS`

**Tipos:** `RECEITA`, `DESPESA`

### Assistente

| Método | Rota | Corpo | Descrição |
|---|---|---|---|
| POST | `/api/assistente` | texto (`text/plain`) | Comando em texto |
| POST | `/api/voz` | `multipart/form-data`, campo `audio` | Comando em áudio |

A rota `/api/voz` responde com a transcrição e a resposta final:

```json
{
  "transcricao": "Gastei 30 reais de Uber hoje.",
  "resposta": "Registrado: despesa de R$ 30,00 em 2026-10-29, categoria TRANSPORTE."
}
```

### Erros

| Status | Quando |
|---|---|
| 400 | Dados inválidos (valor zero, descrição vazia, enum desconhecido, corpo ilegível) |
| 422 | Regra de negócio violada (data futura, período invertido, transação inexistente) |

## Exemplos de uso

```bash
# criar uma despesa
curl -X POST http://localhost:8080/api/transacoes \
  -H "Content-Type: application/json" \
  -d '{"descricao":"Compras da semana","valor":150.75,"tipo":"DESPESA","categoria":"MERCADO"}'

# saldo do mês
curl "http://localhost:8080/api/transacoes/saldo?inicio=2026-10-01&fim=2026-10-31"

# comando em texto
curl -X POST http://localhost:8080/api/assistente \
  -H "Content-Type: text/plain" \
  -d "gastei 45 reais no mercado ontem"

# comando em áudio
curl -X POST http://localhost:8080/api/voz -F "audio=@/caminho/para/gasto.m4a"
```

**Dica para Windows:** ao testar pelo terminal, evite acentos no corpo das requisições (o terminal pode enviá-los em codificação incorreta e a API devolve 400). Com áudio isso não acontece.

## Testes

```bash
./gradlew test
```

Os testes não chamam a IA, então são rápidos e não consomem a cota da Groq:

- `TransacaoServiceTest`: regras de negócio, com o repository simulado
- `PeriodoResolverTest`: cálculo de períodos, incluindo viradas de mês e de ano
- `TransacaoRequestTest`: validações do DTO
- `ProjectApplicationTests`: sobe o contexto da aplicação

O relatório detalhado fica em `build/reports/tests/test/index.html`.

## Decisões de projeto

- **Extração estruturada em vez de Tool Calling.** A primeira versão usava Tool Calling do Spring AI. Os modelos disponíveis no plano gratuito da Groq são de raciocínio, e a Groq rejeita o campo `reasoning_content` que o Spring AI reenvia na segunda chamada. A solução foi pedir ao modelo apenas a extração dos dados em JSON e deixar o código chamar o serviço. Isso também deixa as regras de negócio fora da IA.
- **Valores em `BigDecimal`**, sempre positivos, com o sinal definido pelo tipo da transação.
- **Enums para categorias**, o que impede a IA de inventar categorias.
- **Dupla validação:** Bean Validation no DTO e regras no service, para que a validação valha também para comandos que não passam pelo controller.
- **Chave de API só por variável de ambiente.**

## Limitações

- Os dados são perdidos ao reiniciar (H2 em memória).
- Não há resposta por voz: o plano gratuito da Groq só oferece voz em inglês e árabe.
- Não há autenticação nem separação de dados por usuário.
- Comandos de voz gravam direto, sem pedir confirmação.
- Os limites do plano gratuito da Groq podem mudar e gerar erros `429`.

## Ideias de evolução

- Confirmação antes de gravar comandos por voz, para evitar erro de transcrição
- Persistência em PostgreSQL com migrations
- Resposta falada, devolvendo o texto para o cliente sintetizar a voz
- Novas consultas (maior gasto do mês, comparação entre meses)
- Autenticação e dados por usuário
- Documentação interativa da API com OpenAPI/Swagger
