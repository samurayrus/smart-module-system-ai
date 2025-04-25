# smart-module-system-ai
### Модульная система по взаимодействию с llm. 
Добавляет функции работы с базой данных, проксирования запросов, интерфейс gui и другие

## Подготовка к работе
<details>
  
  1. Для локального развертывания llm скачайте **LM Studio** (возможно, нужен vpn).
  2. В **LM Studio** выберете нужную вам модель. Советую **gemma-3-12B-it-qat-GGUF** если у вас 12gb VRAM или **gemma-3-4b-it-8q** если 8gb VRAM. Модели ниже тоже можно, но они не умеют в анализ изображений. Модель автоматически скачается.
  3. Запустите jar (когда будет релиз) или проект в среде разработки с настроенным **application.yml или additional.yml** (см. GET STARTED)
  4. Пишите в появившийся чат и модель должна вам ответить (если вы используете GUI, если нет, то установите SillyTavern и подключите её к SMSA через localhost:8089/v1)

  Работа с внешними llm по api пока **не реализована**.
  Работа с модулями из внешних интерфейсов через прокси тоже не реализована из-за потери контекста при выполнении. Эта опция **скоро будет доступна**, но в урезанном варианте.
  
</details>

## Описание функционала / GetStarted:

**additional.yml** позволяет редактировать следующую часть:
```yaml
app:
  modules:
    databaseworker:
      enabled: false
      datasource:
        jdbc-url: jdbc:postgresql://xxx
        username: xxx
        password: xxx
        driver-class-name: org.postgresql.Driver
    gui:
      enabled: true
  version: 0.1.0
```

(Также другие технологические занчения, например server.port для прокси)

В **modules** перечислены текущие активируемые модули для работы с llm.
На данный момент доступны для настройки **databaseworker** и **gui**.

### Database Worker
 **Database Worker** подключен к шине воркеров и перехватывает запросы от llm,
которые предназначены для выполнения sql на базе данных.
Позволяет выполнять запросы в цикле с повторной обработкой llm.

**Например:**
```
user: {Просит что-то сделать на бд}
assistant: {Проговаривает что ей нужно сделать, чтобы при длинных цепочках задач не терять цель и пишет sql запрос,
используя для парсинга форму <SQL_START> SQL <SQL_END>}
tool: {Увидел что есть обращение к бд через специальную конструкцию и выполняет sql, после чего возвращает ответ или ошибку со стектрейсом}
assistant: {Обрабатывает ответ. Если ошибка, то пытается исправить или обращается к пользователю, если ничего ен получается.}
```
Дальше либо повторные запросы через **цикл**, либо обращение к пользователю

### Gui Service
**Gui Service** - позволяет общаться через интерфейс на swing. (разная окраска сообщений, мигание новых сообщений и тд).
На данный момент, дополнительно хранит в себе полный контекст, н ов будущем будет перенесено в отдельный сервис.

### Proxy Service
**Proxy Service** - неотключаемый модуль, который поднимается на заданном порту (по умолчанию 8089) 
и отвечает за прокидывание запросов от внешних интерфейсов (например **SillyTavern**) к llm.
В будущем добавится настройка этого модуля.

#### Текущее апи:

#### @GetMapping("/v1/models") return ResponseEntity<String>

(Этот эндпоинт покроет 99% всего взаимодействия с llm)
#### @PostMapping("/v1/chat/completions")  @RequestBody String prompt; return ResponseEntity<String>

#### @PostMapping("/v1/completions") @RequestBody String prompt; return ResponseEntity<String>

#### @PostMapping("/v1/embeddings") @RequestBody String prompt; return ResponseEntity<String>


## Для разработчиков:
Пока нет поддержки внешних плагинов и есть возможность только писать внутренние модули, которые тут называются воркерами.
1. Создать **свой** пакет в ru.samurayrus.smartmodulesystemai.workers
2. Создать класс (желательно @Service) с имплементацией **WorkerListener** и подтягиванием WorkerEventDataBus через конструктор.
3. Реализовать метод boolean **callWorker(String content)** и наполнить его полезной нагрузкой.
   На вход прилетает последнее сообщение, полученное от llm. Вы должны решить, нужно с ним что-нибудь делать или нет.
   Чтобы сохранить запись в контекст, вы должны использовать вызов **guiService.addMessageToPane("tool", message);** 
   Если вернуть true, то последний контекст отправится в llm и снова придет ответ.
   Если вернуть false- значит ответ от llm не требуется (может вы искали нужную структуру в сообщении и не нашли её, на примере DataBaseWorker)
4. Вы должны добавить в yml к другим модулям свои параметры и параметр активации.
5. Добавьте по аналогии к своиму воркеру **@ConditionalOnProperty(prefix = "app.modules.databaseworker", name = "enabled", havingValue = "true")**
6. В методе @PostConstruct вызовете **workerEventDataBus.registerWorker(this)**, чтобы **зарегестрировать** свой воркер в шине.

#### Пример: 
```java
package ru.samurayrus.smartmodulesystemai.workers.your_package;
@Service
@ConditionalOnProperty(prefix = "app.modules.yourmodule", name = "enabled", havingValue = "true")
public class YourWorker implements WorkerListener {
    private final WorkerEventDataBus workerEventDataBus;
    private final GuiService guiService;
    
    @Autowired
    public YourWorker(WorkerEventDataBus workerEventDataBus, GuiService guiService) {
        this.workerEventDataBus = workerEventDataBus;
        this.guiService = guiService;
    }

    @Override
    public boolean callWorker(String content) {
        // Логика обработки контента. Можно посмотреть ан пример поиска тригера в LlmSqlResponseParser
        if (content.contains("<YOUR_TRIGGER>")) {
            guiService.addMessageToPane("tool", "Результат работы модуля");
            return true;  // Отправить результат работы в LLM
        }
        return false;  // Пропустить обработку и не отправлять ответ llm
    }

    @PostConstruct
    public void init() {
        workerEventDataBus.registerWorker(this);
    }
}
```
**Не забудь добавить в yml параметр включения!!!**
```yml
app:
  modules:
    ...
    your_module:
     enabled: true
     ...
  version: 0.1.0
````

Готово! Ваш модуль приступает к работе!
Добавляйте по желанию свои модули, добавляйте функционал поиска в интернете, работы с кафкой, файловой системой или проектами в idea!

#### С ув, SamurayRus (:
