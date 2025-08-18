## Запуск проекта на Linux

### Требования:
- Java 11+
- Maven 3.6+
- Git

### 1. Клонирование репозитория
git clone https://github.com/ZufarFaiz/InterviewTask.git

### 2. Переход в папку с проектом
cd - Название репозитория

### 3. Запуск приложения
mvn clean compile exec:java -Dexec.args="/полный/путь/к/вашему/tickets.json"

