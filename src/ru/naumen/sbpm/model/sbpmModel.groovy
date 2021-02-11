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
//Параметры------------------------------------------------------
//Функции--------------------------------------------------------
//Основной блок -------------------------------------------------
/*
 * Объектная модель
 */
class ObjectModel{
    Classes classes
}

class Classes{
    Type[] types
    Attribute[] attributes
    State[] states
    ActiveStatus[] activeStatuses
}

class Type{
    String metaCode
    String title
    Attribute[] attributes
    State[] states
    ActiveStatus[] activeStatuses
}
class Attribute{
    String metaCode
    String metaClass //это точно string?
    String title

}

class State{
    String metaCode //Почему не просто code?
    String title
}
class ActiveStatus{
    String metaCode //Почему не просто code?
}

/*
 *  Справочники (опер.)
 * это пользовательский класс
 * Почему событие (смена статуса) отдельный атрибут? Нужен ли отдельный класс?
 * Чем code отличается от metaCode ??
 * типа StateEvent нет в ОМ сейчас!
 */
class Catalogs{
    ResolutionCode resolutionCode
    Event event
    StateEvent stateEvent
}

class ResolutionCode{
    String code
    String title
    String description
}
class Event{
    String code
    String title
    String description
}
class StateEvent{
    String code
    String title
    State[] sourceStates //правильно что ссылаюсь к классу State ом?
    ResolutionCode sourceResolutionCode //правильно что использую  ResolutionCode?
}

/*
 * Справочники(техн.)
 * тут же список?
 */
class UserCatalogs{
    CatalogsElement[] source //источник многоэкземплярности шагов (такого справочника нет)
    CatalogsElement prepare //Предварительная обработка параметров
    CatalogsElement[] actionType //Тип действия
}
class CatalogsElement{
    String code
    String tile
    String parent
}