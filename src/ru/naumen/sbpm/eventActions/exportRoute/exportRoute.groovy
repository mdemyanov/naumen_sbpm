import ru.naumen.sbpm.model.*
import ru.naumen.metainfo.shared.ClassFqn
import com.google.gson.Gson

Gson gson = new Gson()
def route = subject
Export ex = modules.sbpmExport.export(route)
def exportToJSON = gson.toJson(ex)

try
{
    byte[] data = exportToJSON.getBytes()
    utils.attachFile(route,'exportFiles', "export ${route.title}.json", 'application/json', 'export', data)
}catch(e){
    def msg = "Возникла проблема при экспорте. Обратитесь к админестратору системы."
    logger.error("BPM EXPORT:  ${e.message}",e)
    utils.throwReadableException(msg,[] as String[], msg,[] as String[]);
}
result.showMessage("Экспорт прошел успешно! Файл экспорта был прикреплен к маршруту.")
