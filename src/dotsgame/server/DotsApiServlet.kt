package dotsgame.server

import zApi.api.context.CallContext
import zApi.api.servlet.ApiServlet
import zApi.info.ApiMethodInfo
import zUtils.badAlgorithm
import zUtils.log.setLogMarker
import zUtils.log.unsetLogMarker
import javax.servlet.GenericServlet
import javax.servlet.ServletConfig
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

//@WebServlet(
//    urlPatterns = ["/a/*"],
//    loadOnStartup = 2
//)
//@MultipartConfig
class DotsApiServlet : GenericServlet() {
    private var servlet0: ApiServlet? = null
    private val servlet: ApiServlet
        get() {
            if (servlet0 == null) {
                servlet0 = object : ApiServlet(appSecret, Context::class) {
                    override fun newContext(
                        req: HttpServletRequest,
                        resp: HttpServletResponse,
                        methodInfo: ApiMethodInfo,
                        params: MutableMap<String, Any?>
                    ): CallContext {
                        return Context.forRequest(req, resp, methodInfo.annotation.basicAuth, params)
                    }
                }
            }
            return servlet0!!
        }

    override fun init() {
        setLogMarker("SInit")
        try {
            servlet.init()
        } finally {
            unsetLogMarker()
        }
    }

    override fun init(config: ServletConfig) {
        setLogMarker("SInit")
        try {
            servlet.init(config)
        } finally {
            unsetLogMarker()
        }
    }

    override fun destroy() {
        setLogMarker("SDestroy")
        try {
            servlet.destroy()
        } finally {
            unsetLogMarker()
        }
    }

    override fun service(req: ServletRequest, res: ServletResponse) {
        servlet0!!.service(req, res)
    }
}
