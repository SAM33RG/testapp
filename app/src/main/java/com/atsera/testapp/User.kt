package com.atsera.testapp

class User {

    var number: String = ""
    var visit_count = 0
    var url: String = ""

    constructor(number: String, visit_count: Int, url: String) {
        this.number = number
        this.visit_count = visit_count
        this.url = url
    }


}