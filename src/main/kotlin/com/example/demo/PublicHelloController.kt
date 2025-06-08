
package com.example.demo

import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/")
class PublicHelloController {

    @GetMapping("/hello")
    fun hello(): String {
        val authentication: Authentication? = SecurityContextHolder.getContext().authentication
        return """
                Hello ${authentication?.name} from protected resource!
                Your roles: ${authentication?.authorities?.joinToString()} 
            """.trimIndent()
    }
}
