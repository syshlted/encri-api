# L25 — Which app receives a handoff plays no part in its name, and the user's tick is asked on *open with* too
*2026-11-11 · accepted on the user's word, with encri D148, which has the reasons; **flagged for veto on one point**
(below). Amends L24, whose companions offered the name on a share alone. Nothing here changes the wire.*

**The receiving app's identity is not consulted.** encri keeps no list of apps that read real names, and a client
that receives a handoff, a companion among them, reads the made-up name like any other app. The handoff is made
before the system's chooser opens, so the app it will reach is not known when the name is decided.

**A client asks for `handOffNamed` on a tick the user makes**, "Send its name", unticked every time and never
remembered, in the dialog it already shows before a handoff: on a share and on *open with* alike. A client that
opens a document without asking sends no name. This is guidance to clients, which encri cannot enforce: a client
holding `RIGHT_READ` can read the name over its session anyway (L11).

*Rejected:* a list of apps the user keeps in encri, which would need encri to recognise the receiving app at its
handoff provider, and would put the decision away from the moment of sending (encri D148). *Rejected:* real names for
any companion that receives a handoff, on the same recognition.

**Flagged for veto:** the tick on *open with*, where a viewer's title bar then shows the real name.
