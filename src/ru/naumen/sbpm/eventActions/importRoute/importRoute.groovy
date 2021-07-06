import ru.naumen.sbpm.model.*
import ru.naumen.metainfo.shared.ClassFqn
import com.google.gson.Gson

Gson gson = new Gson()
def route
Export exportFromJSON

try
{
    byte[] data = utils.readFileContent(sourceObject)
    String exportToJSON =  new String(data, "UTF-8")

    exportFromJSON = gson.fromJson(exportToJSON, Export.class)
    modules.sbpmImport.importBpm(exportFromJSON, subject)
}catch(e){
    def msg = "Возникла проблема при импорте. Подробнее читай в комментариях маршрута."
    logger.error("BPM IMPORT:  ${e.message}",e)
    utils.throwReadableException(msg,[] as String[], msg,[] as String[]);
}