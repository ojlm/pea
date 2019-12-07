package pea.app.compiler

case class CompileResponse(success: Boolean, errMsg: String = null, hasModified: Boolean = true)
