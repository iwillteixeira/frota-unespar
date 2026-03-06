# 🚗 Diário de Bordo — UNESPAR

Sistema web de controle de uso de veículos da frota institucional da UNESPAR.  
O formulário registra saídas e retornos de veículos e envia os dados por e-mail automaticamente.

---

## 📋 Tecnologias

- **Backend:** Java 17 + Spring Boot 3
- **Frontend:** HTML, CSS, JavaScript (vanilla)
- **E-mail:** SMTP Office 365
- **Deploy:** Docker + Heroku
- **Proxy local:** Caddy

---

## ⚡ Rodando localmente (sem Docker)

### Pré-requisitos

- Java 17+
- Maven 3.9+
- [Caddy](https://caddyserver.com/docs/install) (opcional, para proxy)

### 1. Configure as variáveis de ambiente

```bash
cp .env.example .env
# Edite .env com suas credenciais de e-mail
```

### 2. Suba o backend

```bash
cd backend
MAIL_USERNAME=seu_email@unespar.edu.br \
MAIL_PASSWORD=sua_senha \
mvn spring-boot:run
```

O backend estará disponível em `http://localhost:8080`.

### 3. Acesse o frontend

Abra `frontend/index.html` diretamente no navegador **ou** suba o Caddy como proxy:

```bash
sudo caddy start --config /home/juscelinot/frota/Caddyfile
```

Acesse `http://localhost`.

---

## 🐳 Rodando com Docker

### Pré-requisitos

- Docker
- Docker Compose

### 1. Configure as variáveis de ambiente

```bash
cp .env.example .env
# Edite .env com suas credenciais de e-mail
```

### 2. Suba o container

```bash
docker-compose up --build
```

Acesse `http://localhost:8080`.

---

## ☁️ Deploy no Heroku

### Pré-requisitos

- [Heroku CLI](https://devcenter.heroku.com/articles/heroku-cli)
- Docker instalado

### 1. Faça login e crie o app

```bash
heroku login
heroku create nome-do-seu-app
heroku stack:set container -a nome-do-seu-app
```

### 2. Configure as variáveis de ambiente

```bash
heroku config:set MAIL_USERNAME=seu_email@unespar.edu.br -a nome-do-seu-app
heroku config:set MAIL_PASSWORD=sua_senha -a nome-do-seu-app
```

### 3. Faça o deploy

```bash
git push heroku main
```

---

## 🔧 Variáveis de Ambiente

| Variável        | Obrigatória | Descrição                                      |
|-----------------|-------------|------------------------------------------------|
| `MAIL_USERNAME` | ✅ Sim       | E-mail institucional para envio (Office 365)   |
| `MAIL_PASSWORD` | ✅ Sim       | Senha do e-mail institucional                  |
| `PORT`          | ❌ Não       | Porta do servidor (padrão: `8080`)             |
| `CORS_ORIGINS`  | ❌ Não       | Origens CORS permitidas (padrão: `*`)          |

---

## 📁 Estrutura do Projeto

```
frota/
├── frontend/               # Interface web (HTML/CSS/JS)
│   ├── index.html
│   ├── app.js
│   ├── style.css
│   ├── favicon.ico
│   └── icons/
│       └── logo-unespar.png
├── backend/                # API Spring Boot
│   └── src/main/
│       ├── java/           # Código-fonte Java
│       └── resources/
│           ├── application.properties
│           └── static/     # Cópia do frontend (servida pelo Spring Boot)
├── Dockerfile              # Build multi-stage para produção
├── docker-compose.yml      # Ambiente Docker local
├── heroku.yml              # Configuração de deploy no Heroku
├── Caddyfile               # Proxy reverso para desenvolvimento local
└── .env.example            # Modelo de variáveis de ambiente
```

---

## 📬 E-mail

O sistema envia os registros do diário para `juscelino.junior@unespar.edu.br` via SMTP Office 365 (porta 587, STARTTLS).

---

## ⚠️ Observações

- O arquivo `.env` **nunca deve ser commitado** (já está no `.gitignore`).
- Ao modificar arquivos em `frontend/`, sincronize com `backend/src/main/resources/static/`:
  ```bash
  cp -r frontend/* backend/src/main/resources/static/
  ```
- O `Caddyfile` é apenas para uso local — em produção o Spring Boot serve o frontend diretamente.
