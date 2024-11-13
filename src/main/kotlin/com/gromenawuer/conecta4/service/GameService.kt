package com.gromenawuer.conecta4.service

import com.google.api.client.auth.oauth2.Credential
import com.gromenawuer.conecta4.dto.GameOut
import com.gromenawuer.conecta4.dto.GameIn
import com.gromenawuer.conecta4.repository.GameRepository
import com.gromenawuer.conecta4.dto.Games
import com.gromenawuer.conecta4.dto.User
import org.springframework.stereotype.Service

@Service
class GameService(private val gameRepository: GameRepository) {
    fun list(): Games {
        return gameRepository.list()
    }

    fun create(game: GameIn): GameOut {
        return gameRepository.create(game)
    }

    fun get(id: String): GameOut {
        return gameRepository.get(id)
    }

    fun update(id: String, game: GameIn): GameOut {
        return gameRepository.update(id, game)
    }

    fun share(id: String, user: User): GameOut {
        return gameRepository.share(id, user)
    }

    fun login(): Map<String, String>{
        return gameRepository.loginAlt()
    }

    fun exchangeCodeForCredential(code: String): Credential {
        return gameRepository.exchangeCodeForCredential(code)
    }
}