package com.mkonst.components

import com.mkonst.models.ChatOpenAIModel

/**
 * The basis of a component used for generation/fixing/enhancement of Test cases and is requiring 1 model
 * for its operations
 */
abstract class AbstractModelComponent {
    protected var model: ChatOpenAIModel = ChatOpenAIModel()

    open fun getNrRequests(): Int {
        return model.nrRequests
    }

    open fun resetNrRequests() {
        model.nrRequests = 0
    }

    open fun closeConnection() {
        this.model.closeConnection()
    }
}