# guidelines
- if you want to add a feature, ideally reach out with issues first so you don't end up submitting a pr i won't merge.

there are certain things i won't maintain and so i can't instantly merge either:
- new minecraft versions
- dedicated server functionality (which is not well supported and there is no intention for support to be a part of openboatutils itself as it is a client mod)

if you want to contribute something from the above list and you are able to maintain it yourself then please contact me.

# hard rules & other
i have rules for myself that openboatutils *must* follow, which you also need to account for when making contributions:
- *never* do any modifications from vanilla unless the server has sent a packet wanting this behaviour. this involves making sure that, if you add or change a setting, it properly resets, and that it's default behaviour matches vanilla.
- modifications have to work and be tested on every supported minecraft version. at current this means you need to make your modification both in `boatmixin.java` and 1.21.3's `abstractboatmixin.java`, but im going to work on merging these into something more workable. the exception to this rule is modifications intended to make new versions behave like older ones, like interpolation compatibility, where the old versions already have the expected behaviour, these do not need implementations on those versions but still need testing.

this is open source software and i can't really tell you what not to do if you decide to fork, but please read the forking section below.

# forking
i make some opinionated decisions about how openboatutils should be, you are free to fork the project if you disagree but to avoid degrading compatibility please do not use the name openboatutils or the packet channel identifier `openboatutils:settings`. if you do this then the server will be unable to differentiate between which mod the client actually has installed and potentially lead to abusable differences in behaviours that the server would be unaware of. bumping the version identifier would not be enough because i am not going to intentionally step around version IDs that you use, they are all reserved for openboatutils.

this does not apply for forks you intend to merge back in to this repository, but please don't release builds for those either, because you potentially create version IDs that are ambiguous by doing that.
