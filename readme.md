# Запуск приложения
## Локально
`gradlew bootRun`

## В Minikube
### Сборка
`gradlew bootBuildImage`
### Запуск
`minikube image load salving/vk-integration:0.0.1`

`kubectl apply -f deployment.yml`

`minikube tunnel`

---

### Дополнительно
Документация доступна по адресу 
<http://localhost/>
или
<http://localhost:8080/> 
при запуске локально