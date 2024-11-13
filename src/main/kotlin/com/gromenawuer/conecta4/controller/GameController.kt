package com.gromenawuer.conecta4.controller

import com.gromenawuer.conecta4.dto.GameOut
import com.gromenawuer.conecta4.dto.GameIn
import com.gromenawuer.conecta4.service.GameService
import org.springframework.web.bind.annotation.*
import com.gromenawuer.conecta4.dto.Games
import com.gromenawuer.conecta4.dto.User

@RestController
@RequestMapping("/games")
class GameController(private val gameService: GameService) {
    @GetMapping
    fun listGames(): Games {
        return gameService.list()
    }

    @PostMapping
    fun createGames(@RequestBody game: GameIn): GameOut {
        return gameService.create(game)
    }

    @GetMapping("/{id}")
    fun checkGame(@PathVariable id: String): GameOut {
        return gameService.get(id)
    }

    @PutMapping("/{id}")
    fun updateCell(@PathVariable id: String, @RequestBody game: GameIn): GameOut {
        return gameService.update(id, game)
    }

    @PutMapping("/{id}/shares")
    fun share(@PathVariable id: String, @RequestBody user: User): GameOut{
        return gameService.share(id, user)
    }

    @GetMapping("/login")
    fun login(): Map<String, String>{
        return gameService.login()
    }

    @GetMapping("/auth/callback")
    fun handleGoogleCallback(@RequestParam("code") code: String): Map<String, String> {
        val credential = gameService.exchangeCodeForCredential(code)
        return mapOf("status" to "success", "message" to "Authorization complete")
    }
}