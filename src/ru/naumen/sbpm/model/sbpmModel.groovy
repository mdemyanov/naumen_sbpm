package ru.naumen.sbpm.model
/*! UTF8 */
//Автор: mdemyanov,vsapozhnikova
//Дата создания: 05.02.2021
//Код: 
//Назначение:
/**
 * sbpmModel
 */
//Версия: 4.11.*
//Категория:
import ru.naumen.metainfo.shared.ClassFqn
import ru.naumen.common.shared.utils.Color
import ru.naumen.common.shared.utils.DateTimeInterval
//Параметры------------------------------------------------------
//Функции--------------------------------------------------------
//Основной блок -------------------------------------------------
abstract  class Bpm {
    ClassFqn mc
    def obj
    abstract List getSearcher()
}
abstract class MetaStorageAbstract extends Bpm{
    String metaCode
    String title

    abstract List getCreator(def parent = null)
    abstract Map getEditor()

    String getExceptionString(String message) {
        return """Ошибка! При редактировании/создании 
объекта  "${title}(${metaCode})" метакласса ${mc.code} возникла ошибка:
${message}."""
    }
}

abstract class  RouteAbstract extends  Bpm{
    String internalId
    String externalId
    String title

    String getExceptionString(String message){
        return """Ошибка! При создании 
${title ? title : ''}(${internalId}) метакласса ${mc.code} возникла ошибка:
${message}."""
    }
}

class SbpmModel {
    MetaStorage metaStorage
    Catalogs catalogs
    UserCatalogs userCatalogs
    List<AttrKaseToKase> listAttrKaseToKase
}

class Export{
    SbpmModel sbpmModel
    Route route
    List<Template> templates
    List<Action> actions
}


/*
 * Объектная модель
 */

class MetaStorage {
    List<Clazz> clazzes
}

class Clazz extends MetaStorageAbstract {
    List<Attribute> attributes
    List<Status> states
    List<Status> activeStates
    List<Clazz> kases
    String description

    List getSearcher() {
        return [mc.toString(), [metaCode: metaCode]]
    }

    List getCreator(def parent = null) {
        return [mc, [
                metaCode   : metaCode,
                description: description,
                title      : title,
                parent: parent
        ]]
    }

    Map getEditor() {
        return [
                description: description,
                title      : title
        ]
    }

    static Clazz fromObject(
            def obj,
            List<Attribute> attrs,
            List<Status> status,
            List<Clazz> kases
    ) {
        return obj ? new Clazz(
                metaCode : obj.metaCode,
                title: obj.title,
                mc : obj.metaClass,
                attributes: attrs,
                states: status,
                activeStates: obj.activeStates.collect{
                    st -> Status.fromObjectLite(st)
                },
                kases: kases,
                description : obj.description
        ) : null
    }

    static Clazz fromObjectLite(def obj) {
        return obj ? new Clazz(
                mc: obj.metaClass,
                metaCode: obj.metaCode,
                title:  obj?.title
        ) : null
    }

    String getActiveStatesExceptionString(String message){
        return """Ошибка! При редактировании активных статусов 
в объекте  "${title}"(${metaCode}) возникла ошибка: ${message}."""
    }

    String getNotCreateString(Boolean isClazz){
        String name = isClazz ? 'Класс' : 'Тип'
        return """<li>${name}  "${title}" (${metaCode}) не создан.</li>"""
    }
    String getNotElementString(Boolean isClazz){
        String name = isClazz ? 'В класс' : 'В типе'
        return """<li>${name} "${title}"(${metaCode}) не созданы элементы объектной модели.</li>"""
    }
}


class Attribute extends MetaStorageAbstract{
    String code
    String description
    String objType
    String type
    Boolean isCreate

    List getSearcher(def parent) {
        return [mc.toString(), [metaCode: metaCode, parent : parent]]
    }

    List getSearcher() {
        return [mc.toString(), [metaCode: metaCode]]
    }

    List getCreator(def parent = null) {
        return [mc, [
                metaCode   : metaCode,
                code       : code,
                description: description,
                title      : title,
                parent :parent
        ]]
    }

    Map getEditor() {
        return [
                description: description,
                title      : title
        ]
    }

    static Attribute fromObject(def obj){
        return obj ? new Attribute(
                metaCode : obj.metaCode,
                code : obj.code,
                description : obj.description,
                mc : obj.metaClass,
                title: obj.title
        ) : null
    }

    static Attribute fromObjectLite(def obj) {
        return obj ? new Attribute(
                mc: obj.metaClass,
                metaCode: obj.metaCode
        ) : null
    }

    String getNotCreateString(){
        return """<li>Атрибут "${title}"(${code}) не создан.</li>"""
    }

    String getNotTypeString(){
        return """<li>Атрибут "${title}"(${code}). Тип атрибута (${objType}) не соответствует загружаемому (${type}).</li>"""
    }
}

class Status extends MetaStorageAbstract{

    String code //формат:registered
    String description

    List getSearcher() {
        return [mc.toString(), [metaCode: metaCode]]
    }

    List getCreator(def parent = null) {
        return [mc, [
                metaCode   : metaCode,
                code       : code,
                description: description,
                title      : title,
                parent :parent
        ]]
    }

    Map getEditor() {
        return [
                description: description,
                title      : title
        ]
    }

    static Status fromObject(def obj){
        return obj ? new Status(
                metaCode : obj.metaCode,
                code : obj.code,
                title : obj.title,
                description : obj.description,
                mc : obj.metaClass
        ) : null
    }

    static Status fromObjectLite(def obj) {
        return obj ? new Status(
                mc: obj.metaClass,
                metaCode: obj.metaCode
        ) : null
    }

    String getNotCreateString(){
        return """<li>Статус "${title}"(${code}) не создан.</li>"""
    }
}


/*
 *  Справочники (опер.)
 * это пользовательский класс
 * Может сделать верхий клсс элемент справочника??
 */

class Catalogs {
    List<ResolutionCode> resolutionCode
    List<Event> event
    List<StateEvent> eventChangeSt
}

class ResolutionCode extends Bpm{
    String code  //должен быть обязательный уникальным
    String title
    String description
    CatalogsElement systemIcon

    List getSearcher() {
        return [mc.toString(), [code: code]]
    }

    List getCreator(def icon = null) {
        return [mc, [
                code       : code,
                description: description,
                title      : title,
                system_icon :icon
        ]]
    }

    Map getEditor(def icon = null) {
        return [
                description: description,
                title      : title,
                system_icon :icon
        ]
    }

    static ResolutionCode fromObject(def obj){
        return obj ? new ResolutionCode(
                code : obj.code,
                title : obj.title,
                description : obj.description,
                systemIcon: CatalogsElement.fromObjectLite(obj.system_icon),
                mc : obj.metaClass
        ) : null
    }

    static ResolutionCode fromObjectLite(def obj) {
        return obj ? new ResolutionCode(
                mc: obj.metaClass,
                code: obj.code
        ) : null
    }

    String getExceptionString(String message){
        return """Ошибка! При редактировании/создании элемента "${title}"(${code})
 в  справочнике "Коды решения"(${mc.code}) возникла ошибка: ${message}."""
    }
}

class Event extends Bpm{
    String code
    String title
    String description

    List getSearcher() {
        return [mc.toString(), [code: code]]
    }

    List getCreator() {
        return [mc, [
                code       : code,
                description: description,
                title      : title
        ]]
    }

    Map getEditor() {
        return [
                description: description,
                title      : title
        ]
    }

    static Event fromObject(def obj){
        return obj ? new Event(
                code : obj.code,
                title : obj.title,
                description : obj.description,
                mc : obj.metaClass
        ) : null
    }

    static Event fromObjectLite(def obj) {
        return obj ? new Event(
                mc: obj.metaClass,
                code: obj.code
        ) : null
    }

    String getExceptionString(String message){
        return """Ошибка! При редактировании/создании элемента "${title}"(${code}) 
                        в  справочнике "События"(${mc.code}) возникла ошибка: ${message}."""
    }
}

//наследование нужно для Action
class StateEvent extends Event {
    List<Status> sourceState
    ResolutionCode sourceResult

    List getCreator(def resCode = null, List states = []) {
        return [mc, [
                code       : code,
                description: description,
                title      : title,
                sourceResult : resCode,
                sourceState : states
        ]]
    }

    Map getEditor(def resCode = null, List states = []) {
        return [
                description: description,
                title      : title,
                sourceResult : resCode,
                sourceState : states
        ]
    }

    static StateEvent fromObject(def obj){
        return obj ? new StateEvent(
                code: obj.code,
                title : obj.title,
                description : obj.description,
                sourceState : obj.sourceState.collect{
                    st -> Status.fromObjectLite(st)
                },
                sourceResult : ResolutionCode.fromObjectLite(obj.sourceResult),
                mc : obj.metaClass
        ) : null
    }

    static StateEvent fromObjectLite(def obj) {
        return obj ? new StateEvent(
                mc: obj.metaClass,
                code: obj.code
        ) : null
    }

    String getExceptionString(String message){
        return """Ошибка! При редактировании/создании элемента "${title}"(${code}) 
                        в  справочнике "События. Смена статуса"(${mc.code}) возникла ошибка: ${message}."""
    }
}

/*
 * Справочники(техн.)
 */

class UserCatalogs {
    List<CatalogsElement> multiplySource //источник многоэкземплярности шагов
    List<CatalogsElement> prepare //Предварительная обработка параметров
    List<CatalogsElement> actionType //Тип действия
    List<CatalogsElement> systemIcons //Иконки
}

class CatalogsElement extends Bpm{
    String code
    String title
    Boolean folder
    Color color
    List<CatalogsElement> children
    File file


    List getSearcher() {
        return [mc.code, [code: code]]
    }

    Map getEditor(def parent = null){
        return [
                title: title,
                folder : folder,
                color:  color,
                parent: parent
        ]
    }
    List getCreator(def parent = null){
        return [ mc,
                 [
                         title: title,
                         folder : folder,
                         color:  color,
                         code : code,
                         parent: parent
                 ]
        ]
    }

    static CatalogsElement fromObject(def obj, List<CatalogsElement> children = []){
        return obj ? new  CatalogsElement(
                mc: obj.metaClass,
                code :obj.code,
                title : obj.title,
                folder : obj?.folder,
                color : obj.color,
                children: children
        ) : null
    }

    static CatalogsElement fromObjectLite(def obj) {
        return obj ? new CatalogsElement(
                mc: obj.metaClass,
                code: obj.code
        ) : null
    }

    String getExceptionString(String message){
        return """Ошибка! При редактировании/создании 
элемента каталога  "${title}(${code})" метакласса ${mc.code} возникла ошибка:
${message}."""
    }

    String getIconExceptionString(String message){
        return """Ошибка! При добавлении иконки к элементу  "${title}"
справочника ${mc.code} возникла ошибка: ${message}."""
    }

}

/*
 * для импорта Файлов
 */
class File{
    String title
    byte[] data
    String contentType = 'image/png'

}

/*
 * соответствие атрибутов
 * В ОМ это тип класса Action
 */

class AttrKaseToKase {
    //TODO: добавить mc ыефешс
    static ClassFqn mc  = new ClassFqn('action$attrKaseToKase')//api.types.newClassFqn('action$attrKaseToKase')
    Clazz sourceKase
    Attribute sourceAttr
    // справочник actionType
    CatalogsElement action
    //справочник prepare
    CatalogsElement prepare
    Clazz targetKase
    Attribute targetAttr
    String state

    List getSearcher(
            def sourceKase,
            def sourceAttr,
            def action,
            def prepare,
            def targetKase,
            def targetAttr
    ) {
        return [mc.toString(), [
                sourceKase : sourceKase,
                sourceAttr:  sourceAttr,
                action: action,
                prepare: prepare,
                targetKase: targetKase,
                targetAttr:  targetAttr,
                state: state
                ]]
    }


    List getCreator(
            def sourceKase,
            def sourceAttr,
            def action,
            def prepare,
            def targetKase,
            def targetAttr
    ) {
        return [mc, [
                sourceKase : sourceKase,
                sourceAttr:  sourceAttr,
                action: action,
                prepare: prepare,
                targetKase: targetKase,
                targetAttr:  targetAttr,
                state: state
        ]]
    }

    static AttrKaseToKase fromObject(def obj){
        return obj ? new AttrKaseToKase(
                //mc: obj.metaClass,
                sourceKase : Clazz.fromObjectLite(obj.sourceKase),
                sourceAttr : Attribute.fromObjectLite(obj.sourceAttr),
                action : CatalogsElement.fromObjectLite(obj.action),
                prepare : CatalogsElement.fromObjectLite( obj.prepare),
                targetKase: Clazz.fromObjectLite( obj.targetKase),
                targetAttr : Attribute.fromObjectLite(obj.targetAttr),
                state : obj.state.toString()
        ) : null
    }

    String getExceptionString(String message){
        return """Ошибка! При создании соответствия атрибутов возникла ошибка. 
                        Исходный атрибут: ${sourceAttr?.metaCode}. 
                        Исходный тип: ${sourceKase?.metaCode}. 
                        Целевой атрибут атрибут: ${targetAttr?.metaCode}. 
                        Целевой тип: ${targetKase?.metaCode}. 
                        Текст ошибки: ${message}."""
    }
}

//-----------------------------------------------------------------------------------------------------
/*
 * Маршрут
 */

class Route extends RouteAbstract{
    List<ResolutionCode> resolutionCode
    Clazz baseKase
    Attribute resultAttr
    String note

    List getSearcher() {
        return [mc.toString(), [internalId: internalId]]
    }
    List getCreator() {
        def id =  UUID.randomUUID().toString()
        return [mc, [
                internalId   : id,
                externalId: internalId,
                title: title,
                note: note
        ]]
    }
    static Map getEditor(
            List resolutionCode,
            def baseKase,
            def resultAttr
    ) {
        return [
                resolutionCode   : resolutionCode,
                baseKase: baseKase,
                resultAttr: resultAttr
        ]
    }

    static Route fromObject(def obj){
        return obj ? new  Route(
                mc: obj.metaClass,
                internalId :obj.internalId,
                title : obj.title,
                externalId : obj?.externalId,
                resolutionCode : obj.resolutionCode.collect{
                    rc -> ResolutionCode.fromObjectLite(rc)
                },
                resultAttr: obj.resultAttr,
                baseKase: Clazz.fromObjectLite(obj.baseKase),
                note : obj.note
        ) : null
    }

    static Route fromObjectLite(def obj) {
        return obj ? new Route(
                mc: obj.metaClass,
                internalId: obj.internalId
        ) : null
    }

    String getExceptionString(String message){
        return """Ошибка! При редактировании маршрута 
${title}(${internalId})" метакласса ${mc.code} возникла ошибка:
${message}."""
    }
}
/*
 * Шаблон шага
 */

class Template extends  RouteAbstract{
    Route route
    //Атрибут "Тип шага"
    Clazz kase
    // Атрибут "Атрибут связи с головным объектом"
    Attribute sourceAttr
    //Атрибут "Атрибут для хранения результата"
    Attribute resultAttr
    String note
    String coordPoint
    List<ResolutionCode> resolutionCode

    //ПАРАМЕТРЫ ДЛЯ ШАГА из группы атрибутов
    DateTimeInterval resolutionTime //временной интервал
    String description
    UniversalObj responsibleTeam
    UniversalObj responsibleEmployee
    String subj
    List<UniversalObj> members // набор ссылок на БО


    List getCreator(
    ) {
        return [mc, [
                internalId: internalId]]
    }

    List getCreator(
            def route,
            def kase,
            def sourceAttr,
            def resultAttr,
            List resolutionCode
    ) {
        def id =  UUID.randomUUID().toString()

        return [mc, [
                internalId   : id,
                externalId: internalId,
                title: title,
                note: note,
                route: route,
                kase : kase,
                sourceAttr : sourceAttr,
                resultAttr : resultAttr,
                coordPoint : coordPoint,
                resolutionCode : resolutionCode,
                resolutionTime : resolutionTime,
                description : description,
                subj: subj
        ]]
    }

    List getSearcher() {
        return [mc.toString(), [internalId: internalId]]
    }

    static Template fromObject(def obj){
        return obj ? new  Template(
                mc : obj.metaClass,
                internalId : obj.internalId,
                externalId : obj.externalId,
                route : Route.fromObjectLite(obj.route),
                title : obj.title,
                kase : Clazz.fromObjectLite(obj.kase),
                sourceAttr : Attribute.fromObjectLite(obj.sourceAttr),
                resultAttr : Attribute.fromObjectLite(obj.resultAttr),
                note : obj.note,
                coordPoint : obj.coordPoint,
                resolutionCode : obj.resolutionCode.collect{
                    rc -> ResolutionCode.fromObjectLite(rc)
                },
                //ПАРАМЕТРЫ ДЛЯ ШАГА из группы атрибутов
                resolutionTime : obj.resolutionTime,
                description : obj.description,
                responsibleTeam : UniversalObj.fromObject(obj.responsibleTeam),
                responsibleEmployee : UniversalObj.fromObject(obj.responsibleEmployee),
                subj : obj.subj,
                members : obj.members.collect{
                    empl -> UniversalObj.fromObject(empl)
                }
        ) : null
    }

    static Template fromObjectLite(def obj) {
        return obj ? new Template(
                mc: obj.metaClass,
                internalId: obj.internalId
        ) : null
    }

    String getRespEmployeeString(){
        return responsibleEmployee ? """<br/> В атрибуте "Ответственный сотрудник" (responsibleEmployee) 
указн сотрудник ${responsibleEmployee.title}(${responsibleEmployee.uuid}).<br/>""" : ''
    }

    String getRespTeamString(){
        return responsibleTeam ?"""<br/> В атрибуте "Ответственная команда" (responsibleTeam) 
указна команда ${responsibleTeam.title}(${responsibleTeam.uuid}).<br/>""" : ''
    }

    String getMembersString(){
        String res = ''
        members.each {
            member ->
                res+="""<br/> В атрибуте "Участники" (members) указн ${member.title}(${member.uuid}).<br/>"""
        }
        return res
    }

}
/*
 * Объект
 */
class UniversalObj{
    String uuid
    String title

    static UniversalObj fromObject(def obj) {
        return obj ? new UniversalObj(
                uuid: obj.UUID,
                title: obj.title
        ) : null
    }

}

/*
 * Действие
 */

class Action extends RouteAbstract{
    //Атрибут "Исходный шаблон"
    Template sourceTemplate
    //Атрибут "Исходный статус"
    Status sourceState
    //Атрибут "Исходный код решения"
    ResolutionCode sourceResult
    //Атрибут "Исходное событие"
    Event sourceEvent
    //Атрибут "Действие"
    // справочник actionType
    CatalogsElement action
    //Атрибут "Целевой шаблон"
    Template targetTemplate
    //Атрибут "Целевой статус"
    Status targetState
    //Атрибут "Целевой код решения"
    ResolutionCode targetResult
    String state
    String description
    String coordPoint
    String arrCoordPoint
    //Атрибут "Создать экземпляры шага"
    // справочник multiplySource
    CatalogsElement isMultiplyStep
    //Атрибут "По атрибуту головного объекта"
    Attribute multHeadAttr //Настройки ОМ
    //Атрибут "По атрибуту предыдущего шага"
    Attribute multPrevAttr
    //Атрибут "Связь с экземпляром"
    Attribute multStepAttr

    List<AttributeAction> slaveActions

    List getSearcher() {
        return [mc.toString(), [internalId: internalId]]
    }

    List getCreator(
            def sourceTemplate,
            def sourceState,
            def sourceEvent,
            def action,
            def targetTemplate,
            def targetState,
            def targetResult,
            def isMultiplyStep,
            def multHeadAttr,
            def multPrevAttr,
            def multStepAttr
    ) {
        def id =  UUID.randomUUID().toString()
        return [mc, [
                internalId   : id,
                externalId: internalId,
                sourceTemplate : sourceTemplate,
                sourceState : sourceState,
                sourceEvent : sourceEvent,
                action : action,
                targetTemplate : targetTemplate,
                targetState : targetState,
                targetResult : targetResult,
                state : state,
                description : description,
                coordPoint : coordPoint,
                arrCoordPoint : arrCoordPoint,
                isMultiplyStep : isMultiplyStep,
                multHeadAttr : multHeadAttr,
                multPrevAttr : multPrevAttr,
                multStepAttr : multStepAttr
        ]]
    }

    static Action fromObject(def obj, List<AttributeAction> slaveActions = []) {
        return obj ? new Action(
                internalId : obj.internalId,
                externalId : obj.externalId,
                mc : obj.metaClass,
                sourceTemplate : Template.fromObjectLite(obj.sourceTemplate),
                sourceState : Status.fromObjectLite(obj.sourceState),
                sourceResult : ResolutionCode.fromObjectLite(obj.sourceResult),
                sourceEvent : Event.fromObjectLite(obj.sourceEvent),
                action : CatalogsElement.fromObjectLite(obj.action),
                targetTemplate : Template.fromObjectLite(obj.targetTemplate),
                targetState : Status.fromObjectLite(obj.targetState),
                targetResult : ResolutionCode.fromObjectLite(obj.targetResult),
                state : obj.state.toString(),
                description : obj.description,
                coordPoint : obj.coordPoint,
                arrCoordPoint : obj.arrCoordPoint,
                isMultiplyStep : CatalogsElement.fromObjectLite(obj.isMultiplyStep),
                multHeadAttr : Attribute.fromObjectLite(obj.multHeadAttr),
                multPrevAttr : Attribute.fromObjectLite(obj.multPrevAttr),
                multStepAttr : Attribute.fromObjectLite(obj.multStepAttr),

                slaveActions : slaveActions
        ) : null
    }

    static Action fromObjectLite(def obj) {
        return obj ? new Action(
                mc: obj.metaClass,
                internalId: obj.internalId
        ) : null
    }

}

/*
 * Действие с атрибутом
 */

class AttributeAction extends  RouteAbstract{
    //Атрибут "Исходный атрибут"
    Attribute sourceAttr
    //Атрибут "Действие"
    // справочник actionType
    CatalogsElement action
    //Атрибут "Предварительная обработка"
    //справочник prepare
    CatalogsElement prepare
    //Атрибут "Целевой атрибут"
    Attribute targetAttr //Настройки ОМ
    String state
    Action mainAction

    List getSearcher() {
        return [mc.toString(), [internalId: internalId]]
    }
    List getCreator(
            def sourceAttr,
            def action,
            def prepare,
            def targetAttr,
            def mainAction
    ) {
        def id =  UUID.randomUUID().toString()
        return [mc, [
                internalId   : id,
                externalId: internalId,
                sourceAttr : sourceAttr,
                action : action,
                prepare : prepare,
                targetAttr : targetAttr,
                state : state,
                mainAction : mainAction
        ]]
    }

    static AttributeAction fromObject(def obj, List<AttributeAction> slaveActions = []) {
        return obj ? new AttributeAction(
                internalId : obj.internalId,
                externalId : obj.externalId,
                mc : obj.metaClass,
                sourceAttr : Attribute.fromObjectLite(obj.sourceAttr),
                action : CatalogsElement.fromObjectLite(obj.action),
                prepare : CatalogsElement.fromObjectLite(obj.prepare),
                targetAttr : Attribute.fromObjectLite(obj.targetAttr),
                state : obj.state.toString()
        ) : null
    }

    static AttributeAction fromObjectLite(def obj) {
        return obj ? new AttributeAction(
                mc: obj.metaClass,
                internalId: obj.internalId
        ) : null
    }
}

return ''