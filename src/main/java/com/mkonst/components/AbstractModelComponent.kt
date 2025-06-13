package com.mkonst.components

import com.mkonst.config.ConfigYate
import com.mkonst.interfaces.ChatModel
import com.mkonst.models.ChatOpenAIModel
import com.mkonst.models.ModelProvider

/**
 * The basis of a component used for generation/fixing/enhancement of Test cases and is requiring 1 model
 * for its operations
 */
abstract class AbstractModelComponent(modelName: String? = null) {
    protected var model: ChatModel = ModelProvider.get(modelName)

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