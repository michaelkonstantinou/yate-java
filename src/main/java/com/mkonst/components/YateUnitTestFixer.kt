package com.mkonst.components

import com.mkonst.interfaces.YateUnitTestFixerInterface
import com.mkonst.models.ChatOpenAIModel

class YateUnitTestFixer: YateUnitTestFixerInterface {
    private var model: ChatOpenAIModel = ChatOpenAIModel();

    override fun closeConnection() {
        model.closeConnection()
    }
}