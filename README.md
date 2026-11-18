# 💸 WalletPay API

API RESTful para gerenciamento de carteira digital e transferências financeiras Peer-to-Peer (P2P), desenvolvida com **Java 21** e **Spring Boot**. O projeto foca em solucionar desafios críticos de sistemas financeiros, como precisão monetária, atomicidade transacional, controle de concorrência otimista e autenticação stateless.

---

## 🛠️ Tecnologias Utilizadas

* **Linguagem:** Java 21
* **Framework:** Spring Boot (Spring MVC, Spring Data JPA, Spring Security, Spring Validation)
* **Gerenciador de Dependências / Build:** Apache Maven (com Maven Wrapper `mvnw` / `mvnw.cmd`)
* **Banco de Dados:** PostgreSQL 16
* **Migrações de Banco de Dados:** Flyway
* **Autenticação e Autorização:** Spring Security, OAuth2 Resource Server, JWT (RSASSA-PKCS1-v1_5 com pares de chaves RSA) + RBAC (`ROLE_USER`, `ROLE_ADMIN`)
* **Gerenciamento de Variáveis de Ambiente:** Dotenv Java (`io.github.cdimascio:dotenv-java`)
* **Testes:** JUnit 5 (Jupiter), Mockito, Spring Boot Test, Testcontainers (PostgreSQL)
* **Documentação da API:** Springdoc OpenAPI / Swagger UI
* **Conteinerização:** Docker & Docker Compose

---

## 🏗️ Arquitetura e Decisões Técnicas

O sistema segue a **Arquitetura em Camadas** padrão (`Controller ➔ Service ➔ Repository ➔ Model/Domain`):

* **Precisão Monetária:** Uso estrito de `BigDecimal` para todos os saldos de carteira e valores de transações, evitando problemas de precisão com ponto flutuante.
* **Controle de Concorrência:** Bloqueio Otimista (Optimistic Locking) via `@Version` na entidade `Wallet` para lidar com transferências concorrentes e prevenir condições de corrida (race conditions).
* **Atomicidade Transacional:** Limites transacionais rigorosos (`@Transactional`), garantindo execução "tudo ou nada" para débitos, créditos e registro de transações.
* **Trilha de Auditoria Imutável:** Registros completos de auditoria (`DEPOSIT`, `TRANSFER_IN`, `TRANSFER_OUT`) gerados para cada movimentação financeira, suportando extratos paginados.
* **Segurança Stateless:** JWT assinado com chave privada RSA e verificado com chave pública RSA (`src/main/resources/certs/`).

---

## 📋 Requisitos e Pré-requisitos

* **Java Development Kit (JDK):** Versão 21 ou superior
* **Docker e Docker Compose:** Instalados e em execução (para o banco de dados PostgreSQL local e testes de integração com Testcontainers)
* **Maven:** 3.9+ (Opcional — o Maven Wrapper incluso `./mvnw` é recomendado)

---

## ⚙️ Variáveis de Ambiente

A aplicação utiliza o `dotenv-java` para carregar variáveis de ambiente a partir de um arquivo `.env` na raiz do projeto durante a inicialização. Crie um arquivo `.env` na raiz do projeto com as seguintes variáveis:

```env
# Configurações do Banco de Dados
DB_NAME=walletpay-db
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

| Variável | Descrição | Padrão / Exemplo |
| :--- | :--- | :--- |
| `DB_NAME` | Nome do banco de dados PostgreSQL | `walletpay-db` |
| `DB_USERNAME` | Usuário do PostgreSQL | `postgres` |
| `DB_PASSWORD` | Senha do PostgreSQL | `postgres` |

> **Nota:** As chaves RSA para assinatura e verificação do JWT estão localizadas em `src/main/resources/certs/` (`app.pub` e `app.key`).

---

## 🚀 Instalação e Execução

### 1. Clonar o repositório
```bash
git clone https://github.com/pedromolon/walletpay.git
cd walletpay
```

### 2. Configurar variáveis de ambiente
Crie o arquivo `.env` na pasta raiz com as credenciais do seu banco de dados (conforme exibido acima).

### 3. Iniciar o banco de dados PostgreSQL
```bash
docker compose up -d
```

### 4. Executar a aplicação
Utilizando o Maven Wrapper:

* **Linux / macOS:**
  ```bash
  ./mvnw spring-boot:run
  ```
* **Windows:**
  ```cmd
  mvnw.cmd spring-boot:run
  ```

* **Ponto de Entrada da Aplicação:** `com.pedromolon.walletpay.WalletpayApplication`
* **Porta Padrão:** `8080`

O Flyway aplicará automaticamente as migrações do banco de dados na inicialização (`src/main/resources/db/migration/V1__create_intial_tables.sql`).

---

## 📖 Documentação da API e Endpoints

Com a aplicação em execução, você pode interagir com a documentação do Swagger UI:

* **Swagger UI:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
* **OpenAPI JSON:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### Principais Endpoints

#### Autenticação (`/api/v1/auth`) — Público
* `POST /api/v1/auth/register` — Cadastra uma nova conta de usuário (cria automaticamente uma carteira associada).
* `POST /api/v1/auth/login` — Autentica com e-mail/senha e retorna um token JWT (Bearer).

#### Carteira e Transações (`/api/v1/wallets`) — Autenticado (Requer Token Bearer)
* `GET /api/v1/wallets/balance` — Obtém o saldo da carteira do usuário autenticado.
* `GET /api/v1/wallets/statement` — Retorna o histórico paginado de transações (`page`, `size`, `sort`).
* `POST /api/v1/wallets/deposit` — Realiza um depósito na carteira do usuário autenticado.
* `POST /api/v1/wallets/transfer` — Realiza uma transferência da conta do usuário autenticado para um destinatário.

---

## 📜 Scripts e Comandos Disponíveis

| Comando | Descrição |
| :--- | :--- |
| `./mvnw spring-boot:run` | Executa a aplicação Spring Boot localmente. |
| `./mvnw clean compile` | Limpa e compila o código-fonte da aplicação. |
| `./mvnw package` | Empacota a aplicação em um arquivo JAR executável em `target/`. |
| `./mvnw test` | Executa todos os testes unitários e de integração. |
| `docker compose up -d` | Inicia o container do PostgreSQL em segundo plano. |
| `docker compose down` | Para e remove os containers do banco de dados. |
| `docker compose down -v` | Para os containers e remove o volume do banco de dados (`walletpay_data`). |

---

## 🧪 Testes

A suíte de testes inclui tanto testes unitários isolados quanto testes de integração ponta a ponta utilizando Testcontainers (subindo um container PostgreSQL isolado durante a execução dos testes).

Para executar todos os testes:
```bash
./mvnw test
```

### Estrutura de Testes:
* `WalletServiceTest` — Testes unitários para operações de carteira (depósitos, transferências, saldo, extratos) utilizando Mockito.
* `WalletpayApplicationTests` — Carregamento de contexto e testes de integração com Testcontainers (`TestcontainersConfiguration`).

---

## 📁 Estrutura do Projeto

```plaintext
walletpay/
├── .env                                  # Variáveis de ambiente locais
├── docker-compose.yml                    # Definição do container PostgreSQL
├── HELP.md                               # Guia de ajuda do Spring Boot
├── mvnw / mvnw.cmd                       # Scripts do Maven Wrapper
├── pom.xml                               # Dependências e configurações de build do projeto
├── README.md                             # Documentação do projeto
└── src/
    ├── main/
    │   ├── java/com/pedromolon/walletpay/
    │   │   ├── configuration/            # Configuração do OpenAPI / Swagger
    │   │   ├── controller/               # Controllers da API REST (WalletController)
    │   │   ├── dto/                      # Data Transfer Objects (Requisições e Respostas)
    │   │   ├── exception/                # Definições de exceções e GlobalExceptionHandler
    │   │   ├── model/                    # Entidades JPA (User, Role, Wallet, Transaction)
    │   │   ├── repository/               # Repositórios Spring Data JPA
    │   │   ├── security/                 # Segurança, AuthController, TokenService e config de JWT
    │   │   ├── service/                  # Regras de negócio (WalletService)
    │   │   └── WalletpayApplication.java # Ponto de entrada principal do Spring Boot
    │   └── resources/
    │       ├── application.yaml          # Configurações da aplicação
    │       ├── certs/                    # Chaves pública e privada RSA para JWT
    │       └── db/migration/             # Scripts de migração SQL do Flyway
    └── test/
        └── java/com/pedromolon/walletpay/
            ├── TestcontainersConfiguration.java # Configuração do Testcontainers PostgreSQL
            ├── WalletpayApplicationTests.java   # Testes de contexto do Spring Boot
            └── service/WalletServiceTest.java   # Testes unitários de serviços
```

---

## 📄 Licença

TODO: Adicionar informações de licença (ex.: MIT, Apache 2.0). A licença não está especificada no `pom.xml`.
