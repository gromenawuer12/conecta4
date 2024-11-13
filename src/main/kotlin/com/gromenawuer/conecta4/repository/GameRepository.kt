package com.gromenawuer.conecta4.repository

import com.google.api.client.auth.oauth2.Credential
import com.gromenawuer.conecta4.dto.GameOut
import com.gromenawuer.conecta4.dto.GameIn
import com.gromenawuer.conecta4.dto.Games
import com.gromenawuer.conecta4.dto.User


interface GameRepository {
    fun list(): Games
    fun create(game: GameIn): GameOut
    fun get(id: String): GameOut
    fun update(id: String, game: GameIn): GameOut
    fun share(id: String, user: User): GameOut
    fun loginAlt(): Map<String, String>
    fun exchangeCodeForCredential(code: String): Credential
}