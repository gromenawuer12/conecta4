package com.gromenawuer.conecta4.repository

import com.google.api.client.auth.oauth2.Credential
import com.gromenawuer.conecta4.dto.GameOut
import com.gromenawuer.conecta4.dto.GameIn
import com.gromenawuer.conecta4.dto.Games
import com.gromenawuer.conecta4.dto.User
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Profile("test")
@Repository
class GameMemoryRepository : GameRepository {
    val BDD = HashMap<String, GameOut>()
    override fun list(): Games {
        return Games(BDD.values.toList())
    }

    override fun create(game: GameIn): GameOut {
        if (game.name == null) {
            throw IllegalArgumentException("Game Name is Null")
        }
        val newGame = GameOut("iAf2htfG", game.name)
        BDD[game.name] = newGame
        return newGame
    }

    override fun get(id: String): GameOut {
        return BDD[id] ?: throw NoSuchElementException("Element Not Found")
    }

    override fun update(id: String, game: GameIn): GameOut {
        val e = listOf(game.column!!, game.value!!)
        BDD[id]?.board = listOf(BDD[id]?.board!! + e)

        return BDD[id]!!
    }

    override fun share(id: String, user: User): GameOut {
        TODO("Not yet implemented")
    }

    override fun loginAlt(): Map<String, String> {
        TODO("Not yet implemented")
    }

    override fun exchangeCodeForCredential(code: String): Credential {
        TODO("Not yet implemented")
    }
}