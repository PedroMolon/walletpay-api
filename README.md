# 💸 WalletPay API

API RESTful de carteira digital e transferências financeiras Peer-to-Peer (P2P) construída com **Java 21** e **Spring Boot**. O projeto foca na resolução de desafios críticos de sistemas financeiros, como precisão monetária, atomicidade transacional, controle de concorrência otimista e autenticação stateless.

---

## 🛠️ Stack Tecnológica

* **Linguagem:** Java 21
* **Framework:** Spring Boot (Web, Data JPA, Security, Validation)
* **Banco de Dados:** PostgreSQL
* **Migrations:** Flyway
* **Autenticação & Autorização:** JWT (JSON Web Token) + RBAC
* **Testes:** JUnit 5, MockMvc e Testcontainers (PostgreSQL)
* **Documentação:** Springdoc OpenAPI (Swagger UI)
* **Containerização:** Docker & Docker Compose

---

## 🏗️ Arquitetura e Decisões Técnicas

O sistema segue a **Arquitetura em Camadas** (`Controller ➔ Service ➔ Repository ➔ Domain`) com foco nas seguintes boas práticas:

* **Precisão Monetária:** Utilização estrita de `BigDecimal` para manipulação de saldos e valores transacionados, evitando problemas de arredondamento de ponto flutuante.
* **Controle de Concorrência:** Implementação de *Optimistic Locking* (`@Version`) na entidade `Wallet` para mitigar *race conditions* em transferências simultâneas.
* **Atomicidade:** Escopos transacionais rigorosos (`@Transactional`) assegurando operações *all-or-nothing* em débitos e créditos entre contas.
* **Auditoria:** Registro imutável de todas as entradas e saídas (`DEPOSIT`, `TRANSFER_IN`, `TRANSFER_OUT`) para composição de extrato.

---

## 🚀 Como Executar Localmente

### Pré-requisitos
* Java 21+
* Maven 3.9+
* Docker e Docker Compose

### Passo a passo

1. **Clone o repositório:**
   ```bash
   git clone [https://github.com/seu-usuario/walletpay-api.git](https://github.com/seu-usuario/walletpay-api.git)
   cd walletpay-api
   ```

2. **Suba o banco de dados com Docker Compose:**
   ```bash
   docker compose up -d
   ```

3. **Execute a aplicação:**
   ```bash
   ./mvnw spring-boot:run
   ```
(O Flyway executará automaticamente as migrations de schema e carga inicial ao subir a aplicação).

4. **Acesse a documentação da API (Swagger):**
   ```plaintext
   http://localhost:8080/swagger-ui/index.html
   ```

---

## 🧪 Executando os Testes

Os testes de integração utilizam Testcontainers para subir uma instância efêmera e isolada do PostgreSQL via Docker:

   ```bash
   ./mvnw test
   ```

---

## 📌 Roadmap de Desenvolvimento

    [x] Milestone 1: Setup do projeto, Docker Compose e configuração do Testcontainers

    [ ] Milestone 2: Modelagem de domínio (User, Role, Wallet, Transaction), repositórios e migrations Flyway

    [ ] Milestone 3: Segurança stateless com Spring Security, RBAC e autenticação JWT

    [ ] Milestone 4: Core financeiro (Depósitos, Transferências P2P atômicas, Saldo e Extrato paginado)

    [ ] Milestone 5: Testes de integração ponta a ponta e documentação Swagger