package kappa

import scala.reflect.ClassTag


trait KaSpaceContext extends KappaLikeContext {
  this: ContactGraphs with KaSpaceAbstractSyntax with KaSpaceParsers =>

  // -- Constituents of site graph state types --
  type AgentLabel = Double
  type  SiteLabel = Position
  type  LinkLabel = Orientation

  // -- State types --
  type AgentState = KaSpaceAgentState
  type  SiteState = KaSpaceSiteState
  type  LinkState = KaSpaceLinkState


  /** An implicit providing a class tag for [[SiteState]]s. */
  implicit val siteStateClassTag = ClassTag[SiteState](classOf[SiteState])


  /** KaSpace agent state. */
  final case class KaSpaceAgentState(
    agentStateSet: KaSpaceAgentStateSet,
    radius: Option[AgentLabel],
    orientation: Orientation = Orientation(),
    position: Position = Position(0, 0, 0))
      extends KappaLikeAgentState[KaSpaceAgentState] {

    // -- KappaLikeAgentState[KaSpaceAgentState] API --

    @inline def agentType = agentStateSet.agentType

    @inline def label = radius

    @inline def matchesInLongestCommonPrefix(that: KaSpaceAgentState) =
      this.agentStateSet == that.agentStateSet


    // -- Matchable[KaSpaceAgentState] API --

    @inline def matches(that: KaSpaceAgentState) =
      (this.agentStateSet == that.agentStateSet) &&
      Matchable.optionMatches(this.radius, that.radius)(_==_)

    @inline override def isEquivTo[U <: KaSpaceAgentState](that: U): Boolean =
      (this.agentStateSet == that.agentStateSet) && (this.radius == that.radius)

    @inline def join(that: KaSpaceAgentState) =
      if (this.agentStateSet == that.agentStateSet) (this.radius, that.radius) match {
        case (Some(s1), Some(s2)) if s1 == s2 => Some(this)
        case _ => Some(KaSpaceAgentState(agentStateSet, None))
      } else None

    @inline def meet(that: KaSpaceAgentState) =
      if (this.agentStateSet == that.agentStateSet) (this.radius, that.radius) match {
        case (None, _) => Some(that)
        case (_, None) => Some(this)
        case (Some(s1), Some(s2)) => if (s1 == s2) Some(this) else None
      } else None

    @inline def isComplete = agentStateSet.isEmpty || !radius.isEmpty

    // -- Any API --

    @inline override def toString =
      agentStateSet.agentType + (radius map (":" + _) getOrElse "")
  }


  /** KaSpace site state. */
  final case class KaSpaceSiteState(
    siteStateSet: KaSpaceSiteStateSet,
    position: Option[SiteLabel])
      extends KappaLikeSiteState[KaSpaceSiteState] {

    val positionSym = position map siteStateSet.getLabelSym

    // -- KappaLikeSiteState[KaSpaceSiteState] API --

    @inline def siteName = siteStateSet.siteName

    @inline def label = position

    // -- Matchable[KaSpaceSiteState] API --

    @inline def matches(that: KaSpaceSiteState) =
      (this.siteStateSet == that.siteStateSet) &&
      Matchable.optionMatches(this.positionSym, that.positionSym)(_==_)

    @inline override def isEquivTo[U <: KaSpaceSiteState](that: U): Boolean =
      (this.siteStateSet == that.siteStateSet) && (this.positionSym == that.positionSym)

    @inline def join(that: KaSpaceSiteState) =
      if (this.siteStateSet == that.siteStateSet) (this.positionSym, that.positionSym) match {
        case (Some(s1), Some(s2)) if s1 == s2 => Some(this)
        case _ => Some(KaSpaceSiteState(siteStateSet, None))
      } else None

    @inline def meet(that: KaSpaceSiteState) =
      if (this.siteStateSet == that.siteStateSet) (this.positionSym, that.positionSym) match {
        case (None, _) => Some(that)
        case (_, None) => Some(this)
        case (Some(s1), Some(s2)) => if (s1 == s2) Some(this) else None
      } else None

    @inline def isComplete = siteStateSet.isEmpty || !position.isEmpty

    // -- Any API --

    @inline override def toString =
      siteStateSet.siteName + (position map (":" + _) getOrElse "")
  }

  // TODO Add LinkId to KaSpaceLinkState
  /** KaSpace link state. */
  final case class KaSpaceLinkState(
    linkId: Option[LinkId],
    linkStateSet: LinkStateSet,
    orientation: Option[LinkLabel])
      extends KappaLikeLinkState[KaSpaceLinkState] {

    val orientationSym = orientation map linkStateSet.getLabelSym

    // -- KappaLikeLinkState[KaSpaceLinkState] API --

    @inline def id: LinkId = linkId getOrElse (
      throw new IllegalStateException("no link id"))

    @inline def withLinkId(linkId: LinkId): KaSpaceLinkState =
      KaSpaceLinkState(Some(linkId), linkStateSet, orientation)

    @inline def label = orientation

    // -- Matchable[KaSpaceLinkState] API --
    @inline def matches(that: KaSpaceLinkState) =
      Matchable.optionMatches(this.orientationSym, that.orientationSym)(_==_)

    @inline override def isEquivTo[U <: KaSpaceLinkState](that: U): Boolean =
      this.orientationSym == that.orientationSym

    @inline def join(that: KaSpaceLinkState) =
      (this.orientationSym, that.orientationSym) match {
        case (Some(s1), Some(s2)) if s1 == s2 => Some(this)
        case _ => Some(KaSpaceLinkState(None, linkStateSet, None))
      }

    @inline def meet(that: KaSpaceLinkState) =
      (this.orientationSym, that.orientationSym) match {
        case (None, _) => Some(that)
        case (_, None) => Some(this)
        case (Some(s1), Some(s2)) => if (s1 == s2) Some(this) else None
      }

    @inline def isComplete = linkStateSet.isEmpty || !orientation.isEmpty

    // -- Any API --
    @inline override def toString = (linkId, orientation) match {
      case (Some(id), Some(w)) => id + ":" + w
      case (Some(id), None) => id.toString
      case (None, Some(w)) => w.toString
      case (None, None) => ""
    }
  }


  // -- State set types --
  type AgentStateSet = KaSpaceAgentStateSet
  type SiteStateSet = KaSpaceSiteStateSet
  type LinkStateSet = KaSpaceLinkStateSet


  /** Creates an agent state set from a set of agent state names. */
  def mkAgentStateSet(agentStateSet: AgentStateSetName): AgentStateSet =
    KaSpaceAgentStateSet(agentStateSet.agentType, agentStateSet.labels)

  /** Creates a site state set from a set of site state names. */
  def mkSiteStateSet(agentStateSet: AgentStateSet,
                     siteStateSet: SiteStateSetName): SiteStateSet =
    KaSpaceSiteStateSet(siteStateSet.siteName, siteStateSet.labels, agentStateSet)

  /** Creates a link state set from a set of link state names. */
  def mkLinkStateSet(source: SiteStateSet,
                     target: SiteStateSet,
                     stateSet: LinkStateSetName): LinkStateSet =
    KaSpaceLinkStateSet(source, target, stateSet.labels)


  /** KaSpace agent state set. */
  final case class KaSpaceAgentStateSet(
    agentType: AgentTypeName,
    radii: List[AgentLabel])
      extends KappaLikeAgentStateSet {

    // -- KappaLikeLinkStateSet API --

    @inline def labels: List[AgentLabel] = radii
  }


  /** KaSpace site state set. */
  final case class KaSpaceSiteStateSet(
    siteName: SiteName,
    positions: List[SiteLabel],
    agentStateSet: AgentStateSet)
      extends KappaLikeSiteStateSet {
    // Site label symbols
    type SiteLabelSym = Int
    private val labelSyms: Map[SiteLabel, SiteLabelSym] = positions.zipWithIndex.toMap
    @inline def getLabelSym(label: SiteLabel): SiteLabelSym = labelSyms(label)

    // -- KappaLikeSiteStateSet API --

    @inline def labels = positions

    // TODO This should be KappaLikeSiteStateName(siteName, positions.headOption)
    // if what we want to construct is a mixture agent to have KaSim-like semantics.
    @inline def undefinedSite: SiteState =
      AbstractKaSpaceSiteState(siteName, None).toSiteState(agentStateSet)
  }


  /** KaSpace link state set. */
  final case class KaSpaceLinkStateSet(
    source: SiteStateSet,
    target: SiteStateSet,
    orientations: List[LinkLabel])
      extends KappaLikeLinkStateSet {

    // Link label symbols
    type LinkLabelSym = Int
    private val labelSyms: Map[LinkLabel, LinkLabelSym] = orientations.zipWithIndex.toMap
    @inline def getLabelSym(label: LinkLabel): LinkLabelSym = labelSyms(label)

    // -- KappaLikeLinkStateSet API --

    @inline def labels = orientations

    // -- Any API --

    @inline override def toString = orientations.toString
  }
}

