import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

buildscript {
    dependencies {
        classpath(platform("org.springframework.boot:spring-boot-dependencies:3.5.4"))
        classpath("com.fasterxml.jackson.core:jackson-databind")
    }
    repositories {
        mavenCentral()
    }
}

plugins {
    java
    idea
}

group = "com.lachk.inso"
version = "0.1"

tasks.register("fetchTimeline") {
    doLast {
        fetchTimeline()
    }
}

tasks.register("fetchTimelineArchive") {
    doLast {
        fetchTimelineArchive()
    }
}

tasks.register("fetchMedia") {
    doLast {
        fetchMedia()
    }
}

tasks.register("fetchMediaArchive") {
    doLast {
        fetchMediaArchive()
    }
}

tasks.register("findOtherMedia") {
    doLast {
        findOtherMedia()
    }
}

tasks.register("findDuplicatedMediaNamesForItem") {
    doLast {
        findDuplicatedMediaNamesForItem()
    }
}

tasks.register("findDuplicatedMediaLocations") {
    doLast {
        findDuplicatedMediaLocations()
    }
}

fun fetchTimeline(): Unit {
    val cookie: String by project
    val childId: String by project
    val client = HttpClient.newHttpClient()
    val objectMapper = ObjectMapper()
    var page = 1
    do {
        val response = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("https://app.inso.pl/system-api/timeline/posts?page=${page}&categoryId=0&child=${childId}"))
                .header("Cookie", cookie)
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
        val responseBody = response.body()
        if (response.statusCode() != 200) {
            System.err.println(responseBody)
            return
        }
        val value = objectMapper.readValue(responseBody, Map::class.java)
        val items = value.get("items") as List<*>
        val outputFile = file("${rootDir}/output/timeline-${page}.txt")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(responseBody)
        page++
    } while (items.isNotEmpty())
}

fun fetchTimelineArchive(): Unit {
    val cookie: String by project
    val childId: String by project
    val client = HttpClient.newHttpClient()
    val objectMapper = ObjectMapper()
    var page = 1
    do {
        val response = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("https://app.inso.pl/system-api/timeline/posts?page=${page}&categoryId=99&child=${childId}"))
                .header("Cookie", cookie)
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
        val responseBody = response.body()
        if (response.statusCode() != 200) {
            System.err.println(responseBody)
            return
        }
        val value = objectMapper.readValue(responseBody, Map::class.java)
        val items = value.get("items") as List<*>
        val outputFile = file("${rootDir}/output/timeline-archive-${page}.txt")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(responseBody)
        page++
    } while (items.isNotEmpty())
}

fun fetchMedia(): Unit {
    val client = HttpClient.newHttpClient()
    val objectMapper = ObjectMapper()
    var page = 1
    do {
        val inputFile = file("${rootDir}/output/timeline-${page}.txt")
        val timeline = inputFile.readText()
        val value = objectMapper.readValue(timeline, Map::class.java)
        val items = value.get("items") as List<*>
        if (items.isEmpty()) {
            break
        }
        for (item in items) {
            val itemMap = item as Map<*, *>
            val createdAt = itemMap["createdAt"] as Int
            System.out.println("Page ${page}, item ${createdAt}")
            val media = itemMap["media"] as Map<*, *>
            val photos = media["photos"] as List<*>
            for (photo in photos) {
                val photoMap = photo as Map<*, *>
                val name = photoMap["name"] as String
                System.out.println("Page ${page}, item ${createdAt}, photo ${name}")
                val src = photoMap["src"] as Map<*, *>
                val location = if (src.containsKey("full")) src["full"] as String else src["mp4"] as String
                val outputFile = file("${rootDir}/output/timeline/${createdAt}/${name}")
                outputFile.parentFile.mkdirs()
                if (!outputFile.exists()) {
                    val response = client.send(
                        HttpRequest.newBuilder()
                            .uri(URI.create(location))
                            .build(),
                        HttpResponse.BodyHandlers.ofFile(outputFile.toPath())
                    )
                    if (response.statusCode() != 200) {
                        System.err.println("Cannot fetch ${name}")
                        return
                    }
                }
            }
            val attachments = media["attachments"] as List<*>
            for (attachment in attachments) {
                val attachmentMap = attachment as Map<*, *>
                val name = attachmentMap["name"] as String
                System.out.println("Page ${page}, item ${createdAt}, attachment ${name}")
                val location = attachmentMap["url"] as String
                val outputFile = file("${rootDir}/output/timeline/${createdAt}/${name}")
                outputFile.parentFile.mkdirs()
                if (!outputFile.exists()) {
                    val response = client.send(
                        HttpRequest.newBuilder()
                            .uri(URI.create(location))
                            .build(),
                        HttpResponse.BodyHandlers.ofFile(outputFile.toPath())
                    )
                    if (response.statusCode() != 200) {
                        System.err.println("Cannot fetch ${name}")
                        return
                    }
                }
            }
        }
        page++
    } while (true)
}

fun fetchMediaArchive(): Unit {
    val client = HttpClient.newHttpClient()
    val objectMapper = ObjectMapper()
    var page = 1
    do {
        val inputFile = file("${rootDir}/output/timeline-archive-${page}.txt")
        val timeline = inputFile.readText()
        val value = objectMapper.readValue(timeline, Map::class.java)
        val items = value.get("items") as List<*>
        if (items.isEmpty()) {
            break
        }
        for (item in items) {
            val itemMap = item as Map<*, *>
            val createdAt = itemMap["createdAt"] as Int
            System.out.println("Page ${page}, item ${createdAt}")
            val media = itemMap["media"] as Map<*, *>
            val photos = media["photos"] as List<*>
            for (photo in photos) {
                val photoMap = photo as Map<*, *>
                val name = photoMap["name"] as String
                System.out.println("Page ${page}, item ${createdAt}, photo ${name}")
                val src = photoMap["src"] as Map<*, *>
                val location = if (src.containsKey("full")) src["full"] as String else src["mp4"] as String
                val outputFile = file("${rootDir}/output/timeline-archive/${createdAt}/${name}")
                outputFile.parentFile.mkdirs()
                if (!outputFile.exists()) {
                    val response = client.send(
                        HttpRequest.newBuilder()
                            .uri(URI.create(location))
                            .build(),
                        HttpResponse.BodyHandlers.ofFile(outputFile.toPath())
                    )
                    if (response.statusCode() != 200) {
                        System.err.println("Cannot fetch ${name}")
                        return
                    }
                }
            }
        }
        page++
    } while (true)
}

fun findOtherMedia(): Unit {
    val objectMapper = ObjectMapper()
    var page = 1
    do {
        val inputFile = file("${rootDir}/output/timeline-${page}.txt")
        val timeline = inputFile.readText()
        val value = objectMapper.readValue(timeline, Map::class.java)
        val items = value.get("items") as List<*>
        if (items.isEmpty()) {
            break
        }
        for (item in items) {
            val itemMap = item as Map<*, *>
            val createdAt = itemMap["createdAt"] as Int
            val media = itemMap["media"] as Map<*, *>
            val videos = media["videos"] as List<*>
//            val attachments = media["attachments"] as List<*>
            if (videos.isNotEmpty() /*|| attachments.isNotEmpty()*/) {
                System.out.println("Page ${page}, item ${createdAt} - other media found")
            }
        }
        page++
    } while (true)
}

fun findDuplicatedMediaNamesForItem(): Unit {
    val objectMapper = ObjectMapper()
    var page = 1
    do {
        val inputFile = file("${rootDir}/output/timeline-${page}.txt")
        val timeline = inputFile.readText()
        val value = objectMapper.readValue(timeline, Map::class.java)
        val items = value.get("items") as List<*>
        if (items.isEmpty()) {
            break
        }
        for (item in items) {
            val names = mutableListOf<String>()
            val itemMap = item as Map<*, *>
            val createdAt = itemMap["createdAt"] as Int
            val media = itemMap["media"] as Map<*, *>
            val photos = media["photos"] as List<*>
            for (photo in photos) {
                val photoMap = photo as Map<*, *>
                val name = photoMap["name"] as String
                names.add(name)
            }
            val attachments = media["attachments"] as List<*>
            for (attachment in attachments) {
                val attachmentMap = attachment as Map<*, *>
                val name = attachmentMap["name"] as String
                names.add(name)
            }
            if (names.size != HashSet(names).size) {
                System.out.println("Page ${page}, item ${createdAt} - duplicated media names found")
            }
        }
        page++
    } while (true)
}

fun findDuplicatedMediaLocations(): Unit {
    val objectMapper = ObjectMapper()
    val locations = mutableListOf<String>()
    var page = 1
    do {
        val inputFile = file("${rootDir}/output/timeline-${page}.txt")
        val timeline = inputFile.readText()
        val value = objectMapper.readValue(timeline, Map::class.java)
        val items = value.get("items") as List<*>
        if (items.isEmpty()) {
            break
        }
        for (item in items) {
            val itemMap = item as Map<*, *>
            val media = itemMap["media"] as Map<*, *>
            val photos = media["photos"] as List<*>
            for (photo in photos) {
                val photoMap = photo as Map<*, *>
                val src = photoMap["src"] as Map<*, *>
                val location = if (src.containsKey("full")) src["full"] as String else src["mp4"] as String
                locations.add(location)
            }
            val attachments = media["attachments"] as List<*>
            for (attachment in attachments) {
                val attachmentMap = attachment as Map<*, *>
                val location = attachmentMap["url"] as String
                locations.add(location)
            }
        }
        page++
    } while (true)
    if (locations.size != HashSet(locations).size) {
        System.out.println("Duplicated media locations found")
    }
}
