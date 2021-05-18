package ru.naumen.sbpm.exporter

/*! UTF8 */
//Автор: mdemyanov,vsapozhnikova
//Дата создания: 05.02.2021
//Код:
//Назначение:
/**
 * sbpmExport
 */
//Версия: 4.11.*
//Категория:
import ru.naumen.sbpm.model.*
//import ru.naumen.sbpm.importer.*
import ru.naumen.metainfo.shared.ClassFqn
import com.google.gson.Gson //+
import com.google.gson.GsonBuilder
import ru.naumen.core.server.script.spi.ScriptDtObject
import ru.naumen.common.shared.utils.DateTimeInterval
//Параметры------------------------------------------------------
//Функции--------------------------------------------------------

/*
 * для выгрузки "Справочники(техн.)"
 * @param mcStr метакласс экспортируемого справочника (строка)
 * @param parent родитель экспортируемого элемента справочника
 * @return список элементов каталога завернутый в ОМ
 * TODO: хвостовая рекурсия
 */

List<CatalogsElement> exportCatalog(String mcStr, CatalogsElement parentEl = null){

    List<CatalogsElement> res = []

    def parent = parentEl ? utils.findFirst(*parentEl.getSearcher()) : null
    utils.find(mcStr,['parent':parent]).each{
        el ->
            CatalogsElement element= CatalogsElement.fromObject(el)
            if(el?.icon?.size()>0){
                def file =  el?.icon[0]
                element.file = new File(
                        title: file.title,
                        data: utils.readFileContent(file)
                )
            }
            if(parentEl){
                parentEl.children.add(element)
            }else{
                res+=element
            }
            res+= exportCatalog(mcStr, element)

    }
    return res
}

/*
 * для выгрузки классов (типов)
 * @param mcStr метакласс экспортируемых объектов (строка)
 * @param dict параметры для поиска объектов
 * @return список объектов завернутые в ОМ
 */
List<Clazz> exportClazzes(String mcStr, Map dict = [:]){

    return utils.find(mcStr,dict).collect{
        clazz ->
            Clazz.fromObject(
                    clazz,
                    exportAttributes(['parent': clazz]),
                    exportStates(['parent': clazz]),
                    exportClazzes('metaStorage$kase',['parent': clazz])
            )

    }
}

/*
 * для выгрузки атрибутов
 * @param dict параметры для поиска объектов
 * @return список объектов завернутые в ОМ
 */
List<Attribute> exportAttributes(Map dict){
    //return []
    utils.find('metaStorage$attribute',dict).collect{
        attr ->  Attribute.fromObject(attr)
    }
}

/*
 * для выгрузки статусов
 * @param dict параметры для поиска объектов
 * @return список объектов завернутые в ОМ
 */
List<Status> exportStates(Map dict){
    return utils.find('metaStorage$status',dict).collect{
        obj -> Status.fromObject(obj)
    }
}
/*
 * для выгрузки resolutionCode
 * @return список объектов завернутые в ОМ
 */
List<ResolutionCode> exportResolutionCode(){
    return utils.find('catalogs$resolutionCode',[:]).collect{
        obj -> ResolutionCode.fromObject(obj)
    }
}

/*
 * для выгрузки event
 * @return список объектов завернутые в ОМ
 */
List<Event> exportEvent(){
    return utils.find('catalogs$event',[:]).collect{
        event -> Event.fromObject(event)
    }
}
/*
 * для выгрузки StateEvent
 * @return список объектов завернутые в ОМ
 */
List<StateEvent> exportStateEvent(){
    return utils.find('catalogs$eventChangeSt',[:]).collect{
        obj -> StateEvent.fromObject(obj)
    }
}

/*
 * для выгрузки "Соответствие атрибутов" (action$attrKaseToKase)
 * @return список объектов завернутые в ОМ
 */
List<AttrKaseToKase> exportAttrKaseToKase(){
    return utils.find('action$attrKaseToKase',[:]).collect{
        obj -> AttrKaseToKase.fromObject(obj)
    }
}
/*
 * для выгрузки маршрутп
 * @return JSON для выгрузки маршрута
 */
List<Action> exportActions(def route){
    List actions = (route.templates.initActions + route.templates.execActions).flatten().unique()
    return actions.collect{
        obj ->
            Action act = Action.fromObject(obj)
            act.slaveActions = obj.slaveActions.collect{
                sa -> AttributeAction.fromObject(sa)
            }
            return act
    }
}
/*
 * для экспорта
 * @return JSON для выгрузки маршрута
 */
Export export(def route = null){
    return  new Export(
            sbpmModel: new SbpmModel(
                    //выгружаем "Объектная модель"
                    metaStorage: new MetaStorage(
                            clazzes: exportClazzes('metaStorage$clazz',['parent':null])
                    ),
                    //выгружаем "Справочники(техн.)"
                    catalogs: new Catalogs(
                            resolutionCode : exportResolutionCode(),
                            event : exportEvent(),
                            eventChangeSt : exportStateEvent()
                    ),
                    //выгружаем "Справочники(техн.)"
                    userCatalogs : new UserCatalogs(
                            multiplySource: exportCatalog('multiplySource'),
                            prepare : exportCatalog('prepare'),
                            actionType : exportCatalog('actionType'),
                            systemIcons : exportCatalog('system_icons')
                    ),
                    //выгружаем "Соответствие атрибутов"
                    listAttrKaseToKase : exportAttrKaseToKase()
            ),
            //TODO:вынести за создание
            //выгружаем маршрут
            route : Route.fromObject(route),

            //выгружаем шаблоны шага
            templates: route.templates.collect{
                templ -> Template.fromObject(templ)
            },

            //выгружаем действия
            actions: exportActions(route)
    )
}
//Основной блок -------------------------------------------------

/* TODO
        скрипт миграции для internal id маршрутов,
        (проверить что везде)скрипт вычисления значения по умолчанию internal id
        !! internal id у всех сущьностей внутри маршрута
        переписать рекурсию
        +разобраться с иконками
        проверить файлик (как править ОМ на стенде)
        настроить ОМ так, чтоб нельзя было редактировать id , но в интерфейсе видно.
        +написать экспорт правую часть
 */

import ru.naumen.sbpm.model.*

import ru.naumen.metainfo.shared.ClassFqn
import com.google.gson.Gson //+
import com.google.gson.GsonBuilder

Gson gson = new GsonBuilder()
        .create()

def route = utils.get('bpm$2287402')
Export ex = modules.sbpmExport.export(route)
def exportToJSON = gson.toJson(ex)
//logger.error('КАТАЛОГ ОПЕРАТОР'+ exportToJSON)




Export exportFromJSON = gson.fromJson(exportToJSON, Export.class)
modules.sbpmImport.importBpm(exportFromJSON)
