# Author Today Downloader

Очень примитивная качалка для Author.Today. Пока умеет просто скачивать HTML
файлы с картинками. 

Для использования требуется установить
[chromedriver](https://chromedriver.chromium.org/) - в зависимости от системы,
это может быть просто скачивание .exe файла, или `brew cask install
chromedriver`.

Создайте файл с названием `login.properties` в корневой директории проекта, с
содержимым:

```
login=your_author_today_login@email.com
password=your_author_today_password
chromedriver=path to chromedriver
basedir=downloads
```

Соберите проект командой `mvn package`

Запускайте командой:

```
$ java -jar target/at2epub-1.0-SNAPSHOT-jar-with-dependencies.jar --url="https://author.today/reader/60379"
```

Поскольку используется Selenium, процесс может быть нестабильным - иногда по
каким-то причинам скачивание падает и его приходится перезапускать.

После скачивания в директории `downloads` будут HTML файлы и картинки, их можно
собрать в epub при помощи Sigil или Calibre. В планах автоматическая сборка epub. 