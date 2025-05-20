package com.mkonst.components

import com.mkonst.models.ChatOpenAIModel

abstract class AbstractModelComponent {
    protected var model: ChatOpenAIModel = ChatOpenAIModel()

    open fun getNrRequests(): Int {
        return model.nrRequests
    }

    open fun resetNrRequests() {
        model.nrRequests = 0
    }
}