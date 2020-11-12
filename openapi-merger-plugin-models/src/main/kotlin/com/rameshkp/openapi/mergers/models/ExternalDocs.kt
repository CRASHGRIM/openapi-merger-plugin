package com.rameshkp.openapi.mergers.models

import javax.validation.constraints.NotNull

data class ExternalDocs(@field:NotNull(message = "ExternalDocs Url cannot be null") var url: String? = null, var description: String? = null)