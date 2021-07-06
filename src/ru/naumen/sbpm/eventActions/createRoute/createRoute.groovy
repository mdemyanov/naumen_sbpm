import ru.naumen.sbpm.model.*
import ru.naumen.metainfo.shared.ClassFqn
import com.google.gson.Gson

Gson gson = new Gson()
def route
Export exportFromJSON

try{
    byte[] data = utils.readFileContent(params.exportfile[0])
    String exportToJSON =  new String(data, "UTF-8")

    exportFromJSON = gson.fromJson(exportToJSON, Export.class)
    route = modules.sbpmImport.createRoute(exportFromJSON)
}catch(e){
    def msg = "Возникла проблема при импорте. Обратитесь к админестратору системы."
    logger.error("BPM IMPORT:  ${e.message}",e)
    utils.throwReadableException(msg,[] as String[], msg,[] as String[]);
}

utils.attachFile(route, 'importFiles',params.exportfile[0])

result.goToUrl(api.web.open(route))

