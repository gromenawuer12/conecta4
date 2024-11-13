package com.gromenawuer.conecta4.repository

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.Permission
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.*
import com.gromenawuer.conecta4.dto.GameOut
import com.gromenawuer.conecta4.dto.GameIn
import com.gromenawuer.conecta4.dto.Games
import com.gromenawuer.conecta4.dto.User
import com.gromenawuer.conecta4.exception.CapacityExceededException
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Repository
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

private val SCOPES = listOf(DriveScopes.DRIVE, SheetsScopes.SPREADSHEETS, "openid")
private const val CREDENTIALS_FILE_PATH = "credentials/credentials.json"
private val JSON_FACTORY = GsonFactory.getDefaultInstance()
private const val TOKENS_DIRECTORY_PATH = "tokens"
private const val FOLDER_APP_NAME = "Conecta4"
private const val APPLICATION_NAME = "Conecta4"
private val NET_HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
private const val RANGE = "A1:G6"

@Profile("dev")
@Repository
class GameDriveRepository : GameRepository {
    override fun list(): Games {
        val driveService = driveService()

        val folder = findFolderIdByName(driveService)
            ?: throw IllegalArgumentException("Folder 'Conecta4' not found")

        return findFilesWithAppProperties(driveService, folder.id)
    }

    override fun create(game: GameIn): GameOut {
        val driveService = driveService()

        val folderMetadata = com.google.api.services.drive.model.File()
        folderMetadata.setName("Conecta4")
        folderMetadata.setMimeType("application/vnd.google-apps.folder")
        val folder = findFolderIdByName(driveService) ?: driveService.files().create(folderMetadata)
            .setFields("id")
            .execute()

        val fileMetadata = com.google.api.services.drive.model.File()
        fileMetadata.name = game.name
        fileMetadata.mimeType = "application/vnd.google-apps.spreadsheet"
        fileMetadata.parents = listOf(folder.id)
        fileMetadata.appProperties = mapOf(Pair("kinder", "bueno"))

        val spreadsheet = driveService.files().create(fileMetadata)
            .setFields("id, name")
            .execute()

        return GameOut(spreadsheet.id, spreadsheet.name)
    }

    override fun get(id: String): GameOut {
        val (name, values) = getSpreadsheetValuesRangeById(id, RANGE)

        return GameOut(id, name, values)
    }

    override fun update(id: String, game: GameIn): GameOut {
        if (game.column == null || game.value == null) {
            throw IllegalArgumentException("Argument Not Found")
        }
        fillColumnWithValue(id, game.column, game.value)

        val (name, values) = getSpreadsheetValuesRangeById(id, RANGE)

        println("OWNER: " + getOwner(id))
        println("REQUESTER: " + getRequester())

        return GameOut(id, name, values)
    }

    override fun share(id: String, user: User): GameOut {
        val permission = Permission()
            .setType("user")
            .setRole("writer")
            .setEmailAddress(user.email)

        driveService().permissions().create(id, permission)
            .setFields("id")
            .execute()

        val (name, values) = getSpreadsheetValuesRangeById(id, RANGE)

        return GameOut(id, name, values)
    }

    private fun updateCell(spreadsheetId: String, range: String, value: String) {
        val credential = login()
        val sheetsService = getSheetsService(credential)
        val body = ValueRange().setValues(listOf(listOf(value)))

        sheetsService.spreadsheets().values()
            .update(spreadsheetId, range, body)
            .setValueInputOption("RAW")
            .execute()
    }

    private fun fillColumnWithValue(spreadsheetId: String, column: String, value: String) {
        val startRow = 1
        val endRow = 6
        val values = getSpreadsheetValuesRangeById(spreadsheetId, "$column$startRow:$column$endRow").second
        var isChanged = false

        for (row in 6 downTo 1) {
            val cellValue = if (values.all { it.isEmpty() }) listOf() else values[row - 1]
            if (cellValue.isEmpty()) {
                val range = "$column$row"
                updateCell(spreadsheetId, range, value)
                isChanged = true
                break
            }
        }
        if (!isChanged) throw CapacityExceededException("Column is completed")
    }

    private fun getSpreadsheetValuesRangeById(spreadsheetId: String, range: String): Pair<String, List<List<Any>>> {
        val credential = login()
        val sheetsService = getSheetsService(credential)

        val spreadsheet: Spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute()
        val spreadsheetTitle = spreadsheet.properties.title

        val response: ValueRange = sheetsService.spreadsheets().values()
            .get(spreadsheetId, range)
            .execute()
        val values = response.getValues() ?: emptyList()

        return Pair(spreadsheetTitle, values)
    }


    private fun getSheetsService(credential: Credential): Sheets {
        return Sheets.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            JSON_FACTORY,
            credential
        )
            .setApplicationName("Conecta4")
            .build()
    }


    private fun driveService(): Drive {
        return Drive.Builder(
            NET_HTTP_TRANSPORT,
            JSON_FACTORY,
            login()
        )
            .setApplicationName(APPLICATION_NAME)
            .build()
    }

    @Throws(IOException::class)
    private fun login(): Credential {
        val `in`: InputStream = ClassPathResource(CREDENTIALS_FILE_PATH).inputStream

        val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(`in`))

        val flow = GoogleAuthorizationCodeFlow.Builder(
            NET_HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES
        )
            .setDataStoreFactory(FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build()

        val receiver = LocalServerReceiver.Builder().setPort(8888).build()
        return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    }

    private fun findFolderIdByName(driveService: Drive): com.google.api.services.drive.model.File? {
        val result = driveService.files().list()
            .setQ("mimeType='application/vnd.google-apps.folder' and name='$FOLDER_APP_NAME'")
            .setFields("files(id, name)")
            .execute()

        return result.files?.firstOrNull()
    }

    private fun findFilesWithAppProperties(driveService: Drive, folderId: String): Games {
        val query = "'$folderId' in parents and appProperties has { key='kinder' and value='bueno' }"
        val result = driveService.files().list()
            .setQ(query)
            .setFields("files(id, name, owners)")
            .execute()

        val files = result.files
        if (files.isNullOrEmpty()) {
            return Games(listOf())
        }

        val gamesList: List<GameOut> =
            files.map { item -> GameOut(name = item["name"] as String, id = item["id"] as String, board = getSpreadsheetValuesRangeById(item["id"] as String, RANGE).second) }

        return Games(gamesList)
    }

    override fun loginAlt(): Map<String, String> {
        val `in`: InputStream = ClassPathResource(CREDENTIALS_FILE_PATH).inputStream
        val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(`in`))

        val flow = GoogleAuthorizationCodeFlow.Builder(
            NET_HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES
        )
            .setDataStoreFactory(FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build()

        val redirectUri = "http://localhost:8080/games/auth/callback"
        return mapOf("authUrl" to flow.newAuthorizationUrl().setRedirectUri(redirectUri).build())
    }

    override fun exchangeCodeForCredential(code: String): Credential {
        val `in`: InputStream = ClassPathResource(CREDENTIALS_FILE_PATH).inputStream
        val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(`in`))

        val flow = GoogleAuthorizationCodeFlow.Builder(
            NET_HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES
        )
            .setDataStoreFactory(FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build()

        return flow.createAndStoreCredential(
            flow.newTokenRequest(code).setRedirectUri("http://localhost:8080/games/auth/callback").execute(),
            "user"
        )
    }

    private fun getOwner(id: String): String {
        return driveService().files().get(id)
            .setFields("owners")
            .execute()
            .owners
            .firstOrNull()
            ?.emailAddress ?: throw Exception("OWNER NOT FOUND !!!")
    }

    private fun getRequester(): String {
        return driveService().about().get().setFields("user").execute().user.emailAddress
    }

}