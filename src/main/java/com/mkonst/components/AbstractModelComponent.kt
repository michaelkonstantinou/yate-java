package com.mkonst.components

import com.mkonst.models.ChatOpenAIModel

abstract class AbstractModelComponent {
    protected var model: ChatOpenAIModel = ChatOpenAIModel()

    fun getNrRequests(): Int {
        return model.nrRequests
    }

    fun resetNrRequests() {
        model.nrRequests = 0
    }
}