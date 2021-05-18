package ru.naumen.sbpm.importer
/*! UTF8 */
//Автор: mdemyanov,vsapozhnikova
//Дата создания: 05.02.2021
//Код:
//Назначение:
/**
 * sbpmImport
 */
//Версия: 4.11.*
//Категория:
import ru.naumen.sbpm.model.*
import com.google.gson.Gson //+
import com.google.gson.GsonBuilder
import groovy.transform.Field

class CreatorList{
    Map<String, List<Bpm>> database = [:]
    def get(String mc, Map<String,Object> map){
        return database[mc].find{
            def res = true
            map.each{
                k,v ->
                    if(it[k]!=v){
                        res = false
                        return
                    }
            }
            return  res
        }?.obj
    }

    def add(Bpm element){
        def list = []
        String key = element.mc.toString()
        if( database.keySet().contains(key)){
            list = database[key]
        }else{
            database[key] = list
        }
        list.add(element)
    }


}

//Параметры------------------------------------------------------
@Field CreatorList creatorList = new  CreatorList()
@Field List<String> attrsExceptions = ['systemAttachedFiles','@comment','@commentAuthor','@commentPrivate']
@Field Map<String,List<String>> attrsClazzExceptions =['comment': ['source','copiedToFrom']]
//Функции--------------------------------------------------------

def importCatalog(List<CatalogsElement> catalog, def parent = null) {
    catalog.each {
        element ->
            def elementObj = utils.get(*element.searcher)
            try {
                if (elementObj) {
                    elementObj =  utils.editWithoutEventActions(elementObj, element.getEditor(parent))
                } else{
                    elementObj = utils.create(*element.getCreator(parent))
                    element.obj = elementObj
                    creatorList.add(element)
                }
            } catch (Exception e) {
                String msg = element.getExceptionString(e.message)
                throw new Exception(msg)

            }
            if (element.file) {
                importIcon(element, elementObj)
            }
            importCatalog(element.children, parent)
    }
}

def importIcon(CatalogsElement element, def elementObj) {
    try {
        elementObj.icon.each {
            icon ->
                utils.delete(icon.UUID)
        }
        utils.attachFile(elementObj, 'icon', element.file.title, 'image/png', '', element.file.data)
    } catch (Exception e) {
        String msg = element.getIconExceptionString(e.message)
        throw new Exception(msg)
    }
}

List<Clazz> importClazzes(List<Clazz> clazzes){
    return clazzes.findResults {
        clazz ->
            return importClazz(clazz)
    }
}

Clazz importClazz(Clazz clazz, def parent = null) {
    def clazzObj = utils.get(*clazz.searcher)

    def createClazzObj = metaStorageUpdater(clazzObj,clazz, parent)

    if(!api.metainfo.metaClassExists(clazz.metaCode)){
        return Clazz.fromObjectLite(createClazzObj)
    }
    def createAttrs = importAttributes(clazz?.attributes, clazzObj )


    def createStates = importStates(clazz.states, clazzObj)

    importActiveStates(clazz, clazzObj)
    List<Clazz> kases = clazz.kases.findResults {
        ks ->
            importClazz(ks, clazzObj)
    }
    return (!api.metainfo.metaClassExists(clazz.metaCode))
            ? Clazz.fromObjectLite(createClazzObj)
            :(createAttrs?.size() > 0 || createStates?.size() > 0 || (kases)?.size()>0)
            ?Clazz.fromObject(clazzObj,createAttrs,createStates,kases)
            :null
}

List<Attribute> importAttributes(List<Attribute> attrs, def clazzObj) {
    attrs?.findResults {
        attribute ->
            def attributeObj = utils.get(*attribute.getSearcher(clazzObj))
            metaStorageUpdater(attributeObj, attribute, clazzObj)
            List<String> metaCode = attribute.metaCode.split('#')
            if(attrsExceptions.contains(metaCode[1]) ||
                    (attrsClazzExceptions.keySet().contains(metaCode[0]) &&
                            attrsClazzExceptions[metaCode[0]].contains(metaCode[1]))){
                return null
            }
            attribute.type = attribute?.mc.code.substring(1)+attribute?.mc.code[0]
            attribute.isCreate = api.metainfo.checkAttributeExisting(metaCode[0],metaCode[1])
            if(!attribute.isCreate){
                attribute.objType = api.metainfo.getMetaClass(metaCode[0])?.getAttribute(metaCode[1])?.type?.code
            }
            return attribute.isCreate ||
                    attribute.type != attribute.objType
                    ? attribute
                    : null
    }
}

List<Status> importStates(List<Status> states, def clazzObj) {
    states?.findResults{
        status ->
            def statusObj = utils.get(*status.getSearcher())
            metaStorageUpdater(statusObj,status, clazzObj)
            List<String> metaCode = status.metaCode.split('@')
            return api.metainfo.getMetaClass(metaCode[0]).workflow.getStates().code.contains(metaCode[1]) ? null : status
    }
}

def importActiveStates(Clazz clazz, def clazzObj) {
    try {
        List activeStates = searchList(clazz.activeStates)
        utils.editWithoutEventActions(clazzObj, ['activeStates': activeStates])

    } catch (Exception e) {
        throw new Exception(clazz.getActiveStatesExceptionString(e.message))
    }

}

def importResolutionCode(List<ResolutionCode> rcList) {
    rcList.each {
        rc ->
            def rcObj = utils.get(*rc.searcher)
            def icon = getBpmElement(rc.systemIcon)
            try {
                if (rcObj) {
                    utils.editWithoutEventActions(rcObj, rc.getEditor(icon))
                } else {
                    rcObj = utils.create(*rc.getCreator(icon))
                    rc.obj = rcObj
                    creatorList.add(rc)
                }

            } catch (Exception e) {
                throw new Exception(rc.getExceptionString(e.message))
            }
    }
}

def importEvent(List<Event> eventList) {
    eventList.each {
        ev ->
            def eventObj = utils.get(*ev.searcher)
            try {
                if (eventObj) {
                    utils.editWithoutEventActions(eventObj, ev.editor)
                } else {
                    eventObj = utils.create(*ev.creator)
                    ev.obj = eventObj
                    creatorList.add(ev)
                }

            } catch (Exception e) {
                throw new Exception(ev.getExceptionString(e.message))
            }
    }
}

def importStateEvent(List<StateEvent> eventList) {
    eventList.each {
        ev ->
            def eventObj = utils.get(*ev.searcher)
            def sourceResolutionCode =  getBpmElement( ev.sourceResult)
            List states = searchList(ev?.sourceState)
            try {
                if (eventObj) {
                    utils.editWithoutEventActions(eventObj, ev.getEditor(sourceResolutionCode, states))
                } else {
                    eventObj = utils.create(*ev.getCreator(sourceResolutionCode, states))
                    ev.obj = eventObj
                    creatorList.add(ev)
                }

            } catch (Exception e) {
                throw new Exception(ev.getExceptionString(e.message))
            }
    }
}

def importAttrKaseToKase(List<AttrKaseToKase> listAttrKToK) {

    listAttrKToK.each {
        attrKTok ->

            def sourceKase = getBpmElement(attrKTok.sourceKase)
            def sourceAttr = getBpmElement(attrKTok.sourceAttr)
            def action = getBpmElement(attrKTok.action)
            def prepare = getBpmElement(attrKTok.prepare)
            def targetKase = getBpmElement(attrKTok.targetKase)
            def targetAttr = getBpmElement(attrKTok.targetAttr)
            def attrKToKObj = utils.get(*attrKTok.getSearcher(
                    sourceKase,
                    sourceAttr,
                    action,
                    prepare,
                    targetKase,
                    targetAttr
            ))
            try {
                if (!attrKToKObj) {

                    utils.create(attrKTok.getCreator(
                            sourceKase,
                            sourceAttr,
                            action,
                            prepare,
                            targetKase,
                            targetAttr))
                }

            } catch (Exception e) {
                throw new Exception(attrKTok.getExceptionString(e.message))
            }
    }

}
def importRoute(Route route, def routeObj) {
    List resolutionCode = searchList(route.resolutionCode)
    def baseKase = getBpmElement(route.baseKase)
    def resultAttr = getBpmElement(route.resultAttr)

    try {
        obj = utils.editWithoutEventActions(routeObj, Route.getEditor(resolutionCode,baseKase, resultAttr))
    } catch (Exception e) {
        throw new Exception(route.getExceptionString(e.message))
    }
}

def importTemplates(List<Template> templates, def route){
    templates.collect {
        template ->
            def kase = getBpmElement(template.kase)
            def sourceAttr = getBpmElement(template.sourceAttr)
            def resultAttr = getBpmElement(template.resultAttr)
            List resolutionCode = searchList(template.resolutionCode)
            def templateObj = routeCreator(template, template.getCreator(route,kase,sourceAttr,resultAttr,resolutionCode),"шаблона ${template.title}")

    }
}
def importActions(List<Action> actions, def route){

    return actions.collect {

        action ->
            def sourceTemplate = getBpmElement(action.sourceTemplate)
            def sourceState = getBpmElement(action.sourceState)
            def sourceEvent = getBpmElement(action.sourceEvent)
            def act = getBpmElement(action.action)
            def targetTemplate = getBpmElement(action?.targetTemplate)
            def targetState = getBpmElement(action?.targetState)
            def targetResult = getBpmElement(action?.targetResult)
            def isMultiplyStep = getBpmElement(action.isMultiplyStep)
            def multHeadAttr = getBpmElement(action.multHeadAttr)
            def multPrevAttr = getBpmElement(action.multPrevAttr)
            def multStepAttr = getBpmElement(action.multStepAttr)

            def mainAction =routeCreator(action, action.getCreator(
                    sourceTemplate,
                    sourceState,
                    sourceEvent,
                    act,
                    targetTemplate,
                    targetState,
                    targetResult,
                    isMultiplyStep,
                    multHeadAttr,
                    multPrevAttr,
                    multStepAttr
            ), "действия")
            importAttributeAction(action.slaveActions, mainAction)
    }
}

List importAttributeAction(List<AttributeAction> attributeActions, def mainAction){
    return attributeActions.collect {
        attributeAction ->
            def sourceAttr = getBpmElement(attributeAction.sourceAttr)
            def action = getBpmElement(attributeAction.action)
            def prepare = getBpmElement(attributeAction.prepare)
            def targetAttr = getBpmElement(attributeAction.targetAttr)
            routeCreator(attributeAction, attributeAction.getCreator(sourceAttr,action, prepare, targetAttr,mainAction),"действия с атрибутом")

    }
}

List<Clazz> importSbpmModel(SbpmModel sbpmModel) {

    importUserCatalogs(sbpmModel.userCatalogs)
    List<Clazz> creatorObjModel = importClazzes(sbpmModel.metaStorage.clazzes)
    importCatalogs(sbpmModel.catalogs)
    importAttrKaseToKase(sbpmModel.listAttrKaseToKase)
    return creatorObjModel
}

def importUserCatalogs(UserCatalogs userCatalogs) {
    importCatalog(userCatalogs.multiplySource)
    importCatalog(userCatalogs.systemIcons)
    importCatalog(userCatalogs.prepare)
    importCatalog(userCatalogs.actionType)
}

def importCatalogs(Catalogs catalogs) {
    importResolutionCode(catalogs.resolutionCode)
    importEvent(catalogs.event)
    importStateEvent(catalogs.eventChangeSt)
}
def createRoute(Export exportFromJSON){
    def route
    api.tx.call {
        route = utils.create(*exportFromJSON.route.creator)
    }
    return route
}

def importBpm(Export exportFromJSON, def route) {
    List<Clazz> creatorObjModel
    Gson gson = new GsonBuilder()
            .create()

    try {
        creatorObjModel = importSbpmModel(exportFromJSON.sbpmModel)
        if(creatorObjModel?.size()>0){
            String msg = "<ul>"+ creatorObjModel?.collect {
                cl ->
                    CreateClazzLogger(cl,true)
            }.join('')+"</ul>"
            throw new Exception(msg)
        }
        importRoute(exportFromJSON.route, route)
        importTemplates(exportFromJSON.templates, route)
        importTemplatesInfo(exportFromJSON.templates)
        importActions(exportFromJSON.actions,route)
    }catch(e){
        bpmLogger(e.message, route)
        throw (e)
    }
    return route
}

//Вспомогательные--функции--------------------------------------------------------

def metaStorageUpdater(def obj, MetaStorageAbstract meta, def parent = null) {
    try {
        if (obj) {
            obj =  utils.editWithoutEventActions(obj, meta.editor)
            meta.obj = obj
        } else{
            obj = utils.create(*meta.getCreator(parent))
            meta.obj = obj
            creatorList.add(meta)

        }
    } catch (Exception e) {
        throw new Exception(meta.getExceptionString(e.message))

    }
    return obj
}

def routeCreator( RouteAbstract meta, List creator, String title) {
    def obj
    try {
        obj = utils.create(*creator)
        meta.obj = obj
        creatorList.add(meta)
    } catch (Exception e) {
        throw new Exception(meta.getExceptionString(e.message))

    }
    return obj
}

List searchList(List<Bpm> objList){
    def list =  objList.collect {
        obj ->
            return utils.get(*obj.searcher)

    }
    list.removeAll([null])
    return list
}

def getBpmElement(Bpm element){
    return element
            ? creatorList.get(*element.searcher)
            ?: utils.get(*element.searcher)
            ?:null
            : null
}

def bpmLogger(String msg, def object) {
    logger.error("BPM IMPORT:  ${msg}")
    utils.editWithoutEventActions(object, ['@comment': "BPM IMPORT: "+msg], true)
}


String CreateClazzLogger(Clazz cl, Boolean isClazz = false){
    String msg = ""

    if(cl && (cl?.attributes?.size()>0 || cl?.states?.size()>0 || cl?.kases?.size()>0)){
        msg+= cl.getNotElementString(isClazz)
        msg+="<ul>"+cl?.attributes?.collect{
            attr ->
                return  attr.isCreate ? attr.notCreateString : attr.notTypeString
        }.join('')+"</ul>"
        msg+="<ul>"+ cl.states?.collect{
            st ->
                return st.notCreateString
        }.join('')+"</ul>"
        msg+=cl.kases?.collect {
            ks ->
                return """<ul>${CreateClazzLogger(ks)}</ul>"""
        }.join('')

    }else{
        msg+=cl.getNotCreateString(isClazz)
    }
    return msg
}

def importTemplatesInfo(List<Template> templates){
    templates.each {
        template ->
            String note = template.obj.note
            note+= template.respEmployeeString
            note+= template.respTeamString
            note+=template.membersString

            utils.editWithoutEventActions(template.obj,[note : note])
    }
}

//Основной блок -------------------------------------------------
return ''