
package com.example.demo

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class HelloController {

/*
    @GetMapping("/hello")
    fun hello(@AuthenticationPrincipal userDetails: UserDetails?): String = "Hello ${userDetails?.username} from protected resource!"
*/

    @GetMapping("/hello")
    fun hello(): String {
        val authentication = SecurityContextHolder.getContext().authentication
        return """
                Hello ${authentication.name} from protected resource!
                Your roles: ${authentication.authorities.joinToString()} 
            """.trimIndent()
    }
}
