package kappa

import scala.util.parsing.combinator._

/**
 * Parser for the Kappa language
 */
trait Parser extends JavaTokenParsers {
  this: LanguageContext =>

  val agentType  : Parser[AgentType]
  val siteName   : Parser[SiteName]
  // TODO Should the parser for agentState actually parse the agentType too?
  // Same question for siteName and siteState
  // I think there might be a problem with the way we order sites
  val agentState : Parser[AgentStateName]
  val siteState  : Parser[SiteStateName]
  val linkState  : Parser[LinkStateName]

  object AST {
    /* Site graphs */
    abstract class Term
    case class LinkAnnot(lnk: BondLabel, state: (LinkStateName, LinkStateName)) extends Term
    case class Agent(name: AgentType, state: Option[AgentStateName], intf: Intf) extends Term

    type Expr = List[Term]
    type Intf = List[Site]

    case class Site(name: SiteName, int: Option[SiteStateName], lnk: Link)

    abstract class Link
    case object Stub extends Link
    case object Wildcard extends Link
    case object Undefined extends Link
    case class Linked(label: BondLabel) extends Link
    type BondLabel = Int

    // expr : agent [, expr]
    lazy val expr : Parser[Expr] = repsep(agent | linkAnnot, ",")

    lazy val linkAnnot : Parser[LinkAnnot] = bondLabel ~ (":" ~> linkStatePair) ^^
      { case bondLabel ~ state => LinkAnnot(bondLabel, state) }

    lazy val linkStatePair: Parser[(LinkStateName, LinkStateName)] = linkState ~ ("." ~> linkState) ^^ {
      case lstate1 ~ lstate2 => (lstate1, lstate2) }

    // agent : agent_name [: agent_state] [( interface )]
    lazy val agent : Parser[Agent] = agentType ~ opt(":" ~> agentState) ~ opt(interface) ^^
      { case name ~ state ~ intf => Agent(name, state, intf getOrElse List()) }

    // interface : [ site [, interface] ]
    lazy val interface : Parser[Intf] = "(" ~> repsep(site, ",") <~ ")"

    // site : site_name | site : site_state | site ! bond_label
    lazy val site : Parser[Site] = siteName ~ opt(":" ~> siteState) ~ opt(link) ^^ {
      case name ~ state ~ link => Site(name, state, link getOrElse Stub) }

    // TODO Parse wildcards like ![agentType:agentState | _][.[siteName:siteState | _][.[linkState | _]]]
    lazy val link : Parser[Link] = "!" ~> (bondLabel ^^ (Linked(_)) |
                                           "_" ^^ (_ => Wildcard))  |
                                   "?" ^^ (_ => Undefined)

    lazy val bondLabel : Parser[BondLabel] = wholeNumber ^^ (_.toInt)


    /* Contact graphs */
    type ContactGraph = List[CGTerm]

    abstract class CGTerm
    case class CGAgent(name: AgentType, states: List[AgentStateName], intf: CGIntf) extends CGTerm
    case class CGLinkAnnot(lnk: BondLabel, states: List[(LinkStateName, LinkStateName)]) extends CGTerm

    type CGIntf = List[CGSite]
    case class CGSite(name: SiteName, ints: List[SiteStateName], lnks: List[BondLabel])

    def list[T](p: Parser[T]) = "{" ~> rep1sep(p, ",") <~ "}"

    lazy val cgLinkAnnot: Parser[CGLinkAnnot] = bondLabel ~ (":" ~> list(linkStatePair)) ^^ {
      case bondLabel ~ states => CGLinkAnnot(bondLabel, states) }

    lazy val cgAgent: Parser[CGAgent] = agentType ~ opt(":" ~> list(agentState)) ~ opt(cgIntf) ^^ {
      case name ~ states ~ intf => CGAgent(name, states getOrElse List(), intf getOrElse List()) }

    lazy val cgIntf: Parser[CGIntf] = "(" ~> repsep(cgSite, ",") <~ ")"

    lazy val cgSite: Parser[CGSite] =
      siteName ~ opt(":" ~> list(siteState)) ~ opt("!" ~> list(bondLabel)) ^^ {
        case name ~ states ~ links => CGSite(name, states getOrElse List(), links getOrElse List()) }

    lazy val cg: Parser[ContactGraph] = repsep(cgAgent | cgLinkAnnot, ",")
  }

  def simpleParse[T](p: Parser[T], s: String, name: String) = parseAll(p, s) match {
    case Success(ast, _) => ast
    case msg => println(msg); println();
                throw new IllegalArgumentException(
                  "given " + name + " is invalid: " + s)
  }

  def parseSiteGraph(s: String) = simpleParse(AST.expr, s, "site graph")
  def parseContactGraph(s: String) = simpleParse(AST.cg, s, "contact graph")
}

