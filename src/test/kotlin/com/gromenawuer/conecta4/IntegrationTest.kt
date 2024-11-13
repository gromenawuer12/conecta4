package com.gromenawuer.conecta4

import com.gromenawuer.conecta4.dto.GameOut
import com.gromenawuer.conecta4.dto.GameIn
import com.gromenawuer.conecta4.dto.Games
import com.gromenawuer.conecta4.repository.GameRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.test.context.ActiveProfiles


@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTest() {
    @Autowired
    private lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    private lateinit var gameRepository: GameRepository

    private val GAMES =
        listOf(GameOut("iAf2htfG", "Game001"), GameOut("iAf2htfG", "Game123"), GameOut("iAf2htfG", "Game999"))

    @Test
    fun whenListGamesThenListGames() {
        gameRepository.create(GameIn("Game001"))
        gameRepository.create(GameIn("Game123"))
        gameRepository.create(GameIn("Game999"))

        val games = testRestTemplate.getForObject("/games", Games::class.java)

        Assertions.assertThat(games).isNotNull()
        Assertions.assertThat(games.games).containsExactlyInAnyOrderElementsOf(GAMES)
    }

    @Test
    fun createGame() {
        val gameCreated = testRestTemplate.postForObject("/games", GameIn("Game555"), GameOut::class.java)

        Assertions.assertThat(gameCreated).isEqualTo(GameOut("iAf2htfG", "Game555"))
    }

    @Test
    fun getGame() {
        gameRepository.create(GameIn("Game001"))

        val game = testRestTemplate.getForObject("/games/Game001", GameOut::class.java)

        Assertions.assertThat(game).isEqualTo(GAMES[0])
    }

    @Test
    fun updateGame() {
        gameRepository.create(GameIn("Game001"))

        val headers = HttpHeaders().apply {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
        }
        val requestEntity = HttpEntity(GameIn(column = "A", value = "O"), headers)

        val gamePreModified = testRestTemplate.getForObject("/games/Game001", GameOut::class.java)
        val gameModified =
            testRestTemplate.exchange("/games/Game001", HttpMethod.PUT, requestEntity, GameOut::class.java).body
        val game = testRestTemplate.getForObject("/games/Game001", GameOut::class.java)

        Assertions.assertThat(gameModified).isEqualTo(game)
        Assertions.assertThat(gamePreModified).isNotEqualTo(game)
    }
}