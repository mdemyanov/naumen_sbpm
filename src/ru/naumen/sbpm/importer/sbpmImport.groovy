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
import groovy.transform.Field

@Field CreatorList creatorList = new  CreatorList()
@Field List<String> attrsExceptions = ['systemAttachedFiles','@comment','@commentAuthor','@commentPrivate']
@Field Map<String,List<String>> attrsClazzExceptions =['comment': ['source','copiedToFrom']]

/*
 * Хранилище созданных в системе объектов
 */
class CreatorList{
    Map<String, List<Bpm>> database  = [:]

    /*
    * для получения объекта по параметрам
    * @param mc metaCode объекта
    * @param map параметры объекта
    * @return объект
    */
    def get(String mc, Map<String,Object> map){
        return database[mc]?.find{ it ->
            map.every{ k, v -> it[k] == v }
        }?.obj
    }

    /*
    * для добавления объекта
    * @param element созданный объект
    */
    def add(Bpm element){
        String key = element.mc.toString()
        if (!database.containsKey(key))
        {
            database.put(key, [])
        }
        database.get(key).add(element)
    }
}

//Параметры------------------------------------------------------

//Функции--------------------------------------------------------
/*
    * для импорта каталогов
    * @param catalog список элементов каталога
    * @param parent родительный элемент каталога
    */
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
                logger.error("BPM IMPORT:  ${e.message}",e)
                throw new Exception(element.getExceptionString(e.message))

            }
            if (element.file) {
                importIcon(element, elementObj)
            }
            importCatalog(element.children, parent)
    }
}

/*
* для импорта иконки
* @param element элемент каталога, к которому относится иконка
* @param elementObj элемент каталога в системе, к которому относится иконка
 */
def importIcon(CatalogsElement element, def elementObj) {
    try {
        elementObj.icon.each {
            icon ->
                utils.delete(icon.UUID)
        }
        utils.attachFile(elementObj, 'icon', element.file.title, element.file.mimeType, '', element.file.data)
    } catch (Exception e) {
        logger.error("BPM IMPORT:  ${e.message}",e)
        throw new Exception(element.getIconExceptionString(e.message))
    }
}

/*
* для импорта классов
* @param clazzes список классов, которые нужно импортировать
* @return список классов, при импорте которых возниклки проблемы
*/
List<Clazz> importClazzes(List<Clazz> clazzes){
    return clazzes.findResults {
        clazz ->
            return importClazz(clazz)
    }
}

/*
* для импорта класса
* @param clazz  класс, который нужно импортировать
* @param parent  объект класса родителя
* @return класс, если при его импорте возникла проблема
*/
Clazz importClazz(Clazz clazz, def parent = null) {
    def clazzObj = utils.get(*clazz.searcher)

    def createClazzObj = metaStorageUpdater(clazzObj,clazz, parent)

    //если в системе нет класса с таким metacode, то возращаем класс
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

    //если возникли проблемы при импорте атрибутов, статусов или типов, то возращаем класс
    if (createAttrs?.size() > 0 || createStates?.size() > 0 || (kases)?.size()>0)
    {
        return Clazz.fromObject(clazzObj,createAttrs,createStates,kases)
    }
    return null
}

/*
* для импорта атрибутов
* @param attrs  список атрибутов, которые нужно импортировать
* @param clazzObj  объект класса, где лежат атрибуты
* @return список атрибутов, в которых при импорте возникла проблема
*/
List<Attribute> importAttributes(List<Attribute> attrs, def clazzObj) {
    attrs?.findResults {
        attribute ->
            def attributeObj = utils.get(*attribute.getSearcher(clazzObj))
            metaStorageUpdater(attributeObj, attribute, clazzObj)
            List<String> metaCode = attribute.metaCode.split('#')

            //если метакод не имеет формат КЛАСС#КОД, то возращаем атрибут
            if(metaCode.size() != 2){
                attribute.badMetaСode = true
                return attribute
            }

            //если атрибут содержится в исключениях, то не возращаем его
            if(attrsExceptions.contains(metaCode[1]) ||
                    (attrsClazzExceptions.keySet().contains(metaCode[0]) &&
                            attrsClazzExceptions[metaCode[0]].contains(metaCode[1]))){
                return null
            }
            attribute.type = attribute?.mc.code.substring(1)+attribute?.mc.code[0]
            attribute.isNotCreate = api.metainfo.checkAttributeExisting(metaCode[0],metaCode[1])

            //если атрибут создан в системе,
            //то проверяем соответствует ли тип
            if(!attribute.isNotCreate){
                attribute.objType = api.metainfo.getMetaClass(metaCode[0])?.getAttribute(metaCode[1])?.type?.code
            }

            Boolean isRemoved = attribute.obj?.removed

            //если атрибут не архивный и при этом не создан в системе или тип не соответсвует,
            //то возращаем атрибут
            return !isRemoved && (attribute.isNotCreate ||
                    attribute.type != attribute.objType)
                    ? attribute
                    : null
    }
}

/*
* для импорта статусов
* @param states  список статусов, которые нужно импортировать
* @param clazzObj  объект класса, где лежат статусы
* @return список статусов, в которых при импорте возникла проблема
*/
List<Status> importStates(List<Status> states, def clazzObj) {
    states?.findResults{
        status ->
            def statusObj = utils.get(*status.getSearcher())
            metaStorageUpdater(statusObj,status, clazzObj)
            List<String> metaCode = status.metaCode.split('@')
            Boolean isRemoved = status.obj?.removed

            //если статус не архивный и при этом не создан,
            //то возращаем статус
            return !isRemoved && (api.metainfo.getMetaClass(metaCode[0]).workflow.getStates().code.contains(metaCode[1]))
                    ? null
                    : status
    }
}

/*
* для импорта активных статусов
* @param clazz  класс, из которого импортируем активные статусы
* @param clazzObj  объект класса, в который импортируем активные статусы
*/
def importActiveStates(Clazz clazz, def clazzObj) {
    try {
        List activeStates = searchList(clazz.activeStates)
        utils.editWithoutEventActions(clazzObj, ['activeStates': activeStates])

    } catch (Exception e) {
        logger.error("BPM IMPORT:  ${e.message}",e)
        throw new Exception(clazz.getActiveStatesExceptionString(e.message))
    }

}

/*
* для импорта кодов решения
* @param rcList  список кодов решения
*/
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
                logger.error("BPM IMPORT:  ${e.message}",e)
                throw new Exception(rc.getExceptionString(e.message))
            }
    }
}

/*
* для импорта событий
* @param eventList  список событий
*/
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
                logger.error("BPM IMPORT:  ${e.message}",e)
                throw new Exception(ev.getExceptionString(e.message))
            }
    }
}

/*
* для импорта событий по статусам
* @param eventList  список событий по статусам
*/
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
                logger.error("BPM IMPORT:  ${e.message}",e)
                throw new Exception(ev.getExceptionString(e.message))
            }
    }
}

/*
* для импорта соответствия атрибутов
* @param listAttrKToK  список соответствия атрибутов
*/
def importAttrKaseToKase(List<AttrKaseToKase> listAttrKToK) {

    listAttrKToK.each {
        attrKTok ->

            def sourceKase = getBpmElement(attrKTok.sourceKase)
            def sourceAttr = getBpmElement(attrKTok.sourceAttr)
            def action = getBpmElement(attrKTok.action)
            def prepare = getBpmElement(attrKTok.prepare)
            def targetKase = getBpmElement(attrKTok.targetKase)
            def targetAttr = getBpmElement(attrKTok.targetAttr)
            def attrKToKObj = utils.findFirst(*attrKTok.getSearcher(
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
                logger.error("BPM IMPORT:  ${e.message}",e)
                throw new Exception(attrKTok.getExceptionString(e.message))
            }
    }

}

/*
* для импорта маршрута
* @param route маршрут
* @param routeObj объект маршрута, в котором будем производить импорт
*/
def importRoute(Route route, def routeObj) {
    List resolutionCode = searchList(route.resolutionCode)
    def baseKase = getBpmElement(route.baseKase)
    def resultAttr = getBpmElement(route.resultAttr)

    try {
        obj = utils.editWithoutEventActions(routeObj, Route.getEditor(resolutionCode,baseKase, resultAttr))
    } catch (Exception e) {
        logger.error("BPM IMPORT:  ${e.message}",e)
        throw new Exception(route.getExceptionString(e.message))
    }
}

/*
* для импорта шагов
* @param templates  список шагов
* @param route маршрут
*/
def importTemplates(List<Template> templates, def route){
    templates.collect {
        template ->
            def kase = getBpmElement(template.kase)
            def sourceAttr = getBpmElement(template.sourceAttr)
            def resultAttr = getBpmElement(template.resultAttr)
            List resolutionCode = searchList(template.resolutionCode)
            routeCreator(template, template.getCreator(route,kase,sourceAttr,resultAttr,resolutionCode))
    }
}

/*
* для импорта действий
* @param actions  список действий
*/
def importActions(List<Action> actions){

    return actions.collect {

        action ->
            def sourceTemplate = getBpmElement(action.sourceTemplate)
            def sourceState = getBpmElement(action.sourceState)
            def sourceEvent = getBpmElement(action.sourceEvent)
            def sourceResult = getBpmElement(action.sourceResult)
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
                    sourceResult,
                    act,
                    targetTemplate,
                    targetState,
                    targetResult,
                    isMultiplyStep,
                    multHeadAttr,
                    multPrevAttr,
                    multStepAttr
            ))
            importAttributeAction(action.slaveActions, mainAction)
    }
}

/*
* для импорта действий с атрибутами
* @param attributeActions список действий с атрибутами
* @param mainAction основное действие, с которым связаны действия с атрибутоми
*/
List importAttributeAction(List<AttributeAction> attributeActions, def mainAction){
    return attributeActions.collect {
        attributeAction ->
            def sourceAttr = getBpmElement(attributeAction.sourceAttr)
            def action = getBpmElement(attributeAction.action)
            def prepare = getBpmElement(attributeAction.prepare)
            def targetAttr = getBpmElement(attributeAction.targetAttr)
            routeCreator(attributeAction, attributeAction.getCreator(sourceAttr,action, prepare, targetAttr,mainAction))
    }
}

/*
* для импорта объектной модели
* @param sbpmModel объектная модель
* @return список классов, при импорте которых возникли проблемы
*/
List<Clazz> importSbpmModel(SbpmModel sbpmModel) {

    importUserCatalogs(sbpmModel.userCatalogs)
    List<Clazz> creatorObjModel = importClazzes(sbpmModel.metaStorage.clazzes)
    importCatalogs(sbpmModel.catalogs)
    importAttrKaseToKase(sbpmModel.listAttrKaseToKase)
    return creatorObjModel
}

/*
* для импорта пользовательских каталогов
* @param userCatalogs пользовательсие каталоги
*/
def importUserCatalogs(UserCatalogs userCatalogs) {
    importCatalog(userCatalogs.multiplySource)
    importCatalog(userCatalogs.systemIcons)
    importCatalog(userCatalogs.prepare)
    importCatalog(userCatalogs.actionType)
}

/*
* для импорта  каталогов
* @param catalogs каталоги
*/
def importCatalogs(Catalogs catalogs) {
    importResolutionCode(catalogs.resolutionCode)
    importEvent(catalogs.event)
    importStateEvent(catalogs.eventChangeSt)
}

/*
* для создания маршрута
* @param importFromJSON все данные для импорта
* @return созданный объект маршрута
*/
def createRoute(Export importFromJSON){
    def route
    api.tx.call {
        route = utils.create(*importFromJSON.route.creator)
    }
    return route
}

/*
* для импорта информации в маршрут
* @param importFromJSON все данные для импорта
* @param route объект маршрута, в который будем импортировать информацию
*/
def importBpm(Export importFromJSON, def route) {
    List<Clazz> creatorObjModel

    try {
        creatorObjModel = importSbpmModel(importFromJSON.sbpmModel)
        if(creatorObjModel?.size()>0){
            String msg = "<ul>"+ creatorObjModel?.collect {
                cl ->
                    CreateClazzLogger(cl,true)
            }.join('')+"</ul>"
            throw new Exception(msg)
        }
        importRoute(importFromJSON.route, route)
        importTemplates(importFromJSON.templates, route)
        importTemplatesInfo(importFromJSON.templates)
        importActions(importFromJSON.actions)
    }catch(e){
        bpmLogger(e.message, route)
        logger.error("BPM IMPORT: ${e.message}",e)
        throw (e)
    }
    return route
}

//Вспомогательные--функции--------------------------------------------------------

/*
* для обновления объекта в соответствии информации о нем из importFromJSON
* @param obj объект
* @param meta представление объекта в importFromJSON
* @param parent объект родитель для обновляемого объекта
*/
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
        logger.error("BPM IMPORT:  ${e.message}",e)
        throw new Exception(meta.getExceptionString(e.message))

    }
    return obj
}

/*
* для создания объекта в соответствии информации о нем из importFromJSON
* @param meta представление объекта в importFromJSON
* @param creator список параметров для создания
* @return созданный объект
*/
def routeCreator( RouteAbstract meta, List creator) {
    def obj
    try {
        obj = utils.create(*creator)
        meta.obj = obj
        creatorList.add(meta)
    } catch (Exception e) {
        logger.error("BPM IMPORT:  ${e.message}",e)
        throw new Exception(meta.getExceptionString(e.message))

    }
    return obj
}

/*
* для поиска объекта в соответствии информации о них из importFromJSON
* @param metaList список представление объектов в importFromJSON
* @return список объектов
*/
List searchList(List<Bpm> metaList){
    def list =  metaList.collect {
        meta ->
            return utils.get(*meta.searcher)

    }
    list.removeAll([null])
    return list
}

/*
* для получения объекта в соответствии информации о нем из importFromJSON
* @param meta представление объекта в importFromJSON
* @return объект
*/
def getBpmElement(Bpm meta){

    if( meta){
        //если сущьность есть в хранилище созданных объектов,
        //то берем объект из него
        if(creatorList.get(*meta.searcher)){
            return creatorList.get(*meta.searcher)
        }

        //если сущьность находится в системе,
        //то берем объект из системы
        if(utils.get(*meta.searcher)){
            return utils.get(*meta.searcher)
        }
    }
    return null
}

/*
* для логирования ошибок при импорте
* @param msg текст ошибки
* @param object объект, в который зафиксируем комментарий об ошибке
*/
def bpmLogger(String msg, def object) {
    logger.error("BPM IMPORT:  ${msg}")
    utils.editWithoutEventActions(object, ['@comment': "BPM IMPORT: "+msg], true)
}

/*
* для создания сообщения об ошибке при импорте класса
* @param cl класс, в котором возникла проблема
* @param isClazz класс или тип
* @return текст ошибки
*/
String CreateClazzLogger(Clazz cl, Boolean isClazz = false){
    String msg = ""

    //Если в классе есть несозданные атрибуты или статусы или типы, то логируем информацию о них
    if(cl && (cl?.attributes?.size()>0 || cl?.states?.size()>0 || cl?.kases?.size()>0)){
        //фиксируем, что в классе есть несозданные элементы
        msg+= cl.getNotElementString(isClazz)

        //фиксируем инфу о проблемных атрибутах
        msg+="<ul>"+cl?.attributes?.collect{
            attr ->
                    if(attr.isNotCreate){
                        return attr.notCreateString
                    }
                    if(attr.badMetaСode){
                        return attr.badMetaСode
                    }
                return  attr.notTypeString
        }.join('')+"</ul>"

        //фиксируем инфу о несозданных статусах
        msg+="<ul>"+ cl.states?.collect{
            st ->
                return st.notCreateString
        }.join('')+"</ul>"

        //фиксируем инфу о проблемных типах
        msg+=cl.kases?.collect {
            ks ->
                return """<ul>${CreateClazzLogger(ks)}</ul>"""
        }.join('')

    //иначе не создан сам класс
    }else{
        msg+=cl.getNotCreateString(isClazz)
    }
    return msg
}

/*
* для добавления комментариев в шаги с информацией
* о неимпортированных атрибутов шага
* @param templates список шагов
*/
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