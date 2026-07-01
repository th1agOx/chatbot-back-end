# Docker

Docker é uma plataforma de containerização que empacota aplicações e suas dependências em containers leves e portáteis.

## Conceitos Fundamentais

- **Imagem**: Template read-only com o sistema operacional, aplicação e dependências (ex: `postgres:17`).
- **Container**: Instância executável de uma imagem. Isolado por namespaces e cgroups.
- **Dockerfile**: Script que define como construir uma imagem.
- **Docker Compose**: Ferramenta para definir e rodar múltiplos containers com um único arquivo YAML.
- **Volume**: Persistência de dados fora do ciclo de vida do container.
- **Network**: Rede virtual que permite comunicação entre containers.

## Comandos Essenciais

- `docker ps`: Lista containers em execução.
- `docker images`: Lista imagens locais.
- `docker build -t nome:tag .`: Constrói uma imagem.
- `docker run -d -p 8080:8080 nome:tag`: Roda um container em background.
- `docker compose up -d`: Sobe todos os serviços definidos no docker-compose.yml.
- `docker logs -f <container>`: Acompanha logs de um container.

## Docker Compose

Arquivo YAML que define serviços, redes e volumes:

```yaml
services:
  postgres:
    image: postgres:17
    environment:
      POSTGRES_DB: mydb
      POSTGRES_PASSWORD: secret
    ports:
      - "5433:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

## Boas Práticas

- Usar multi-stage builds para imagens menores.
- Não rodar containers como root.
- Usar .dockerignore para excluir arquivos desnecessários.
- Preferir imagens oficiais e com versão específica (evitar `latest`).
- Separar dados voláteis de persistentes com volumes.
