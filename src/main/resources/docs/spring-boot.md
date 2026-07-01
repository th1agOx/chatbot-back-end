# Spring Boot

Spring Boot é um framework Java que simplifica a criação de aplicações stand-alone, production-grade com configuração mínima. Ele faz parte do ecossistema Spring e utiliza o conceito de autoconfiguração.

## Principais Características

- **Autoconfiguração**: Baseado nas dependências do classpath, o Spring Boot configura automaticamente beans necessários (DataSource, JPA, Web MVC, etc).
- **Starters**: Dependências pré-empacotadas que agrupam bibliotecas comuns (spring-boot-starter-web, spring-boot-starter-data-jpa).
- **Embedded Server**: Tomcat, Jetty ou Undertow embutidos — não é necessário deploy em servidor externo.
- **Actuator**: Endpoints para monitoramento e métricas da aplicação em produção.
- **Externalized Configuration**: Propriedades via application.yml, variáveis de ambiente, argumentos de linha de comando.

## Anotações Essenciais

- `@SpringBootApplication`: Combina `@Configuration`, `@EnableAutoConfiguration` e `@ComponentScan`.
- `@RestController`: Define um controller REST com retorno direto de objetos (serializados para JSON).
- `@Service`: Marca um componente de serviço (camada de negócio).
- `@Repository`: Marca um componente de acesso a dados.
- `@Transactional`: Gerencia transações de banco de dados.

## Ciclo de Vida

1. `ApplicationContext` é inicializado.
2. `CommandLineRunner` e `ApplicationRunner` executam lógica de inicialização.
3. Servidor web embutido inicia.
4. A aplicação está pronta para receber requisições.

## Profile

Permite configurações específicas por ambiente (dev, prod, test) usando arquivos application-{profile}.yml.
