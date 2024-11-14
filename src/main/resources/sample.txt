---
title: Between business and open source ― a case of Embulk
emoji: "🧬️" # アイキャッチとして使われる絵文字（1文字だけ）
type: "idea" # tech: 技術記事 / idea: アイデア
topics: [ "embulk", "opensource", "ecosystem" ]
layout: default
published: false
description: A case-study discussion on a conflict between business and open source activities
---

(This is a translation from the author's own article written in Japanese: [ビジネスとオープンソースの狭間で 〜 Embulk の場合 (前編)](https://zenn.dev/dmikurube/articles/between-business-and-opensource-1))

2023 was a difficult year for the relationship between business and open source.

In June, there was a discussion about the relationship between business and open source, especially among some software engineers who have been deeply involved in open source, sparked by the layoffs at Cookpad of two full-time Ruby committers who were doing research and development for the company.

[Link: Cookpad to discontinue Ruby interpreter development - let's help Koichi and Mame land a new job or support them via GH sponsors (reddit.com)](https://www.reddit.com/r/ruby/comments/142b07v/cookpad_to_discontinue_ruby_interpreter/)

In August, HashiCorp also made headlines by moving its own open source products to the Business Source License 1.1 (BSL).

[Link: HashiCorp adopts Business Source License (hashicorp.com)](https://www.hashicorp.com/blog/hashicorp-adopts-business-source-license)

2023 was also a year in which that Large Language Models (LLMs) became a hot topic throughout the year. It has had significant business implications.

Even if we focus on the relationship between Large Language Models and open source, there are still issues to be addressed, such as "the problem that language models released under non-open source licenses are often called 'open source'" [^open-source-llm] and "the problem that language models tend to generate clones of open source code without complying with license requirements".

[^open-source-llm]: Personally, I have a question about what is the "source" in a language model that is the result of learning from many texts written by someone else.

[Link: Open Source AI: Establishing a common ground (opensource.org)](https://opensource.org/blog/open-source-ai-establishing-a-common-ground)

[Link: GitHub Copilot, which automatically completes the continuation of the source code, points out that ``copyrighted code is output'' (gigazine.net)](https://gigazine.net/gsc_news/en/20221018-github-copilot-emit-copyrighted-code/)

I, the author of this article, work for [Treasure Data](https://www.treasuredata.com/) (TD), which is a company that states ["Open Source is in our DNA"](https://www.treasuredata.com/opensource/). I have been the maintainer of [Embulk](https://www.embulk.org/), an open-source software from TD, for 6 to 7 years after taking over from its original author. I think I am probably the longest timer as "an employee of TD maintaining an open-source software released by TD".

This article is a random essay to share my experience as a maintainer of an open source software from a company for years, while employed by the company.

About Embulk
=============

Embulk and Treasure Data
-------------------------

[Embulk](https://www.embulk.org/) is a software released as open source [^apache-license-2] in 2015 by Sada, who is a co-founder of Treasure Data. It transfers large row-column data from a variety of databases and storage into another database or storage, in bulk.

[^apache-license-2]: Licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).

Embulk is not only an open-source project, but also runs inside TD. [Data Connector](https://docs.treasuredata.com/articles/#!pd/Integrations-Hub-Overview) is a service in TD, that runs Embulk on TD's servers to import data from customers' environments into TD, and to export query results from TD into customers' environments.

Embulk is tightly coupled inside TD as Data Connector. This is a big difference from [Fluentd](https://www.fluentd.org/), which was primarily installed in customer environments. For a long time since I joined TD in 2016, my main role had been to develop and run Data Connector, while also maintaining the open-source Embulk.

On the other hand, Data Connector is a service that runs on TD's servers. It is not able to access network-unreachable environments. In such a case, we sometimes recommend customers to use the open-source Embulk within the customer's environment.

Embulk and plugins
-------------------

Embulk is designed to be "pluggable" with plugins to support a variety of sources and destinations.

Various plugins are developed and released separately from the Embulk core (`embulk-core`), such as [`embulk-input-s3`](https://github.com/embulk/embulk-input-s3) to read data (files) from Amazon S3, [`embulk-parser-csv`](https://github.com/embulk/embulk-parser-csv) to parse CSV files, [`embulk-output-mysql` and `embulk-output-postgresql`](https://github.com/embulk/embulk-output-jdbc) to write data into MySQL or PostgreSQL as a destination.

In fact, Embulk can do almost nothing only by its core. Plugins perform the actual tasks of reading and writing data. Plugins play the essential role of Embulk. In other words, Embulk is a "plugin-dependent" software.

This is in contrast to popular "pluggable software" in the world, whose plugins are usually positioned as "additional features".

Ecosystem and Treasure Data
============================

Not all of these plugins are developed and maintained by TD. A large number of non-TD software engineers have developed and released plugins (many of them as open source). They, together as a whole, form an "ecosystem" of Embulk.

Within TD services, we sometimes use plugins developed by non-TD engineers. We have sometimes recommended our customers to use the open-source Embulk in their environments, as mentioned above, which sometimes includes the use of plugins developed by non-TD engineers. This means that TD is also "a user of the Embulk ecosystem".

![The Embulk ecosystem](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/kcolll9en6xekzdnrcwn.png)

TD is the company that released "the core of a software called Embulk" to the world, but TD does not own, nor control "the Embulk ecosystem". It is true that as of March 2024, only TD employees (for now, mainly me) have the committer privileges for the Embulk core. However, if, for example, TD were to force a reckless change to Embulk with the exclusive committer privileges, the Embulk ecosystem would be spoiled by non-TD plugin developers, and the value of the Embulk ecosystem would be nothing.

TD (still) occupies the committer privileges for the core. However, non-TD plugins could stop working if we made changes to the open-source Embulk core with only the in-house Data Connector in mind, without considering the open source ecosystem. In fact, this happened several times in the early days. Based on this reflection, we have recently been tried making decisions in "the community" loosely formed among active plugin developers and the core maintainer (me), especially about changes that might affect the compatibility between the core and plugins.

Unfortunately, despite the efforts of the community, almost no one in TD understood the relationship and the dynamics.

We often get feature requests for Embulk and Data Connector within TD. Such requests are healthy by themselves, and improve software in general, not only open source.

On the other hand, the requested core feature may inevitably affect existing plugins, even if it looks like an easy change from the sidelines. So sometimes I have to answer "This feature is expected to impact the compatibility with existing plugins, so it is not easy to implement it soon. We need to discuss it with the community."

Then, we get looks like "Can't you do it right away?", "Why do we have to take care of the community?", and "Isn't Embulk owned by TD?" Or, we may get a push for breaking changes in the open-source Embulk like "It would make (our in-house infrastructure) faster" even from our fellow engineer.

This kind of relationship in open source, where things are not closed within a company, can be hard to understand for non-software engineers, or those who are unfamiliar with open source. Even for engineers, it is hard to imagine the reality if they are out of the team, or the project. At the same time, however, I am a member of an organization that states "Open Source is in our DNA." I have been feeling a bit frustrated and wondering if this is the right thing to do. (Note: As mentioned below, the situation in TD has somehow improved somehow with respect to Embulk.)

Sustaining the ecosystem
-------------------------

Well, to keep the Embulk ecosystem maintained, it is not enough to just silently struggle with the Embulk core code (`embulk-core`).

For example, Embulk has to keep up with updates of Java, which is the execution platform of Embulk. Just confirming that the Embulk core works with the new Java is not enough. We have to go through a series of processes to iterate over many plugins, to find out plugins that do not work with the new Java, to identify the conditions under which they do not work, to make decisions about whether the problem should be absorbed by the core or by each plugin, to announce it to plugin developers if necessary, to communicate with them individually, ... and so on. The upgrade from Java 8 to 9 was a particularly big gap. It was only recently that I was able to just say "It should be okay with Java 9+ now...?" [^support-for-newer-java]

[^support-for-newer-java]: ["Embulk v0.11 is coming soon" -- Support for newer Java](https://www.embulk.org/articles/2023/04/13/embulk-v0.11-is-coming-soon.html#support-for-newer-java)

Embulk used JRuby, so we had to consider updating JRuby as well. Java is a platform that cares a lot about compatibility, while JRuby is not so much concerned about compatibility (as the JRuby runtime implementation, not as the Ruby language). In particular, the interface between Java and Ruby has often introduced incompatibilities. [^jruby-constructor]

[^jruby-constructor]: For example, ["JRuby used to accept conditional switching of `super()` calls in the constructor of a Ruby class that extends a Java class, but this is no longer accepted as of JRuby 9.3"](https://github.com/jruby/jruby/wiki/CallingJavaFromJRuby#subclassing-a-java-class). I could "understand" this, but we had to introduce [an incompatible change](https://github.com/embulk/embulk/blob/v0.11.2/embulk-ruby/lib/embulk/error.rb#L6-L49) in Embulk.

It is difficult for a single maintainer to keep up with this kind of information about JRuby. We decided to drop JRuby support from the front line, but even the decision to "drop" JRuby was not easy. We had to determine the steps to take, the period to migrate, the upper limit of support, and the point to give up, through technical experiments, managing communication, and finding out the line that was acceptable to the community... It was also only recently that we began to see the landing point. [^eep-6]

[^eep-6]: The efforts around this part are documented in [EEP-6: JRuby as Optional](https://github.com/embulk/embulk/blob/master/docs/eeps/eep-0006.md).

In addition, Embulk's architecture was fragile against changes as "some of existing plugins were expected to stop working simply by upgrading the libraries used in the core, even if the upgrade was not affected by known incompatibilities of those libraries". Of course, we cannot stay with old versions for long because libraries in the world often have vulnerabilities. [^log4shell] On the other hand, it was not realistic for Embulk to keep the librarires up-to-date in such a situation, like a compatibility bomb with a burning fuse.

[^log4shell]: You may remember [Log4Shell](https://en.wikipedia.org/wiki/Log4Shell).

Being frustrated, we decided to renovate the architecture. However, the rearchitecture required at least one major compatibility-breaking change. On the other hand, it is obviously impossible to migrate all plugins against such a breaking change. We had to look for an incremental approach to migrate them, to communicate with the plugin developers outside of TD, and to prepare to migrate the in-house Embulk environments in parallel, ... We ended up taking three years from the beginning of this plan ([v0.10.0](https://github.com/embulk/embulk/releases/tag/v0.10.0)) to the end ([v0.10.50](https://github.com/embulk/embulk/releases/tag/v0.10.50)). [^td-tech-talk-2022-eep-7]

[^td-tech-talk-2022-eep-7]: I published [a blog post about this](https://api-docs.treasuredata.com/blog/embulk-in-td/), and documented these things as [EEP-7: Core Library Dependencies](https://github.com/embulk/embulk/blob/eep-core-library-dependencies/docs/eeps/draft-core-library-dependencies.md).

Even so, feature requests continue to come in from inside and outside TD, in parallel. We have many issues to consider while communicating in and out. For example... Is there a way to implement the feature without breaking compatibility? What are acceptable migration plans (for whom?) even if we have to break compatibility? Even if the feature doesn't break compatibility immediately, will the new feature interfere with possible future enhancements?

Tax burden to "maintain" a middleware
--------------------------------------

Maintaining a "middleware" like Embulk, which serves as "the underlying foundation for running software developed by other developers", has many difficulties.

It would have been much easier if Embulk were not open source, but just in-house software. Upgrading the libraries, the biggest bottleneck, should not have taken three years because we could have just updated all the libraries of all the plugins at once.

Maintaining "an open-source middleware" is not doable "on the side" of desired feature development because the number of stakeholders tends to increase in an open-source middleware. "Being a maintainer" of such a middleware is totally different from just contributing to an existing open-source project. The major parts of a maintainer's duties are communication and decision making, indeed.

Here is a quote from an article written about "the Linux maintainer burnout problem".

> But, as Corbet mentioned, almost no one -- no one -- pays people to be maintainers. Maintenance is extra work they do on top of their day job. Companies want their programmers to produce new code. They're not interested in paying them to help manage the entire Linux, or any other open-source project, fundamental infrastructure.
>
> The result? Cobet quoted Steve Rostedt, a Google software engineer and maintainer of several Linux kernel projects "[Being a maintainer myself with a full-time job that is not to do my maintainership](https://lore.kernel.org/all/30c87cc1-4b9b-6f8f-361c-aa814e750ee7@suse.de/T/) I'm struggling to find time to work on this."
>
> ["What Linux kernel maintainers do and why they need your help"](https://www.zdnet.com/article/what-linux-kernel-maintainers-do-and-why-they-need-your-help/) (Written by Steven J. Vaughan-Nichols, Oct. 25, 2023 at 10:16 a.m. PT)

Of course, Embulk is not as large as Linux. Its stakeholders are not as diverse as Linux's. Nevertheless, I know something about the following quotes: "almost no one -- no one -- pays people to be maintainers", "Maintenance is extra work they do on top of their day job", and "Being a maintainer myself with a full-time job that is not to do my maintainership." Even TD, which released Embulk to the world, was no exception.

Business and open source
-------------------------

When I joined TD and took over the maintenance of Embulk, TD was not yet large, and the personnel system was not very formalized. My main job at that time was development and operations around Data Connector. Maintaining Embulk formed (sort of) part of the Data Connector job.

However, after [the acquisition](https://blog.treasuredata.com/blog/2018/08/02/the-next-chapter/) and [the separation](https://blog.treasuredata.com/blog/2020/09/14/nvidia-to-acquire-arm/), maintenance of the open-source software that accompanied the development of the products was left behind the organization as the company grew larger and the personnel evaluation system evolved. Software engineers are now evaluated based on their contribution to "Treasure Data's services", the products. At the same time, the open-source Embulk is unfortunately not "Treasure Data's service".

Having said that, my boss entrusted me with "everything about open source". I was able to fit the open-source Embulk maintenance work into my regular weekday job when it could be justified as "necessary for Data Connector". (It took more time than the bare minimum required if it were not open source.)

Indeed, the "extra time-consuming" open source work was never personnel-evaluated, nor even acknowledged. Furthermore, all the work to maintain open source could not be "justified" as part of the Data Connector job. For example, if I worked on "a part in Embulk that is not used by Data Connector" in my regular weekday job, how can I answer a question like "How did this work of yours make Data Connector better?" Of course, I cannot afford to answer "I made nothing (for Data Connector)". [^td-tech-talk-2022-blog]

[^td-tech-talk-2022-blog]: I wrote things around this in the blog post: ["Embulk in TD, and in the future"](https://api-docs.treasuredata.com/blog/embulk-in-td/).

Even worse, the situation discouraged me from asking team members to do open source-related work. Everyone knows that internally closed tasks would be more valued. I myself continued to contribute to open source because I was a bit emotionally involved in open source. In contrast, I could not ask them to "contribute to open source as well" while I was not in a position to evaluate them, and I knew it could be "unpaid work" for them. So open source work became "the shortest straw" in the company.

Even I wish to be excused from spending my private time on continuing the "not very well paid" maintenance work on the software "just assigned to me", which is not incubated from scratch by myself. I do not intend to devote myself entirely to the software just assigned to me, even if it is open source. I want to spend my private time on something I personally prefer.

Let's switch the point of view to the Embulk ecosystem. Treasure Data, the company that released Embulk, the occupant of comitter privileges to the Embulk core, and the employer of me, can be "a good contributor who can suggest and provide both good use cases and implementations for Embulk" if they behave well. At the same time, Treasure Data is in a delicate position where they can be "a rude user who pushes reckless requests", or "a destroyer who messes up the Embulk ecosystem".

The open source work was "the shortest straw" in the company, despite its position on Embulk. It meant that the TD organization was not incentivizing the employees and the team to "behave well" and "be good contributors" to Embulk. In other words, TD was not fulfilling its obligation to the Embulk ecosystem, while someone should have been paying the "tax".

Who pays the tax?
------------------

If you've read this article so far, many of you may be thinking, "Why didn't you just stop maintaining Embulk as open source?" We've actually raised this idea many times, even by myself a few times.

For example, if we had forked Embulk as part of Data Connector and developed it internally as closed source [^closed-development-open-source], we could have developed it much more efficiently as "Treasure Data's service", and our work could have been more properly evaluated. It would have been much easier to make changes that broke compatibility at the code level. There seemed to be no reason not to do so, if we only considered Treasure Data's business and services. We could leave the open-source Embulk as-is, entrust it to someone else if they are interested in maintaining it, or just let them fork it again.

[^closed-development-open-source]: In fact, it is not necessary to make it closed. It could still be open source under an open source license. We could just declare "Its development process is closed, and its upstream is in-house", "We do not accept pull requests", and "We do not guarantee compatibility" to accomplish this purpose.

When Presto split into PrestoDB and PrestoSQL (now Trino) a few years ago [^presto-fork], we heard some disappointed and cynical voices in the company about the decision. But that was nothing special. Something similar could always happen in the company. [^presto-fork-impression]

[^presto-fork]: [Presto Software Foundation Launches to Advance Presto Open Source Community](https://www.prweb.com/releases/presto-software-foundation-launches-to-advance-presto-open-source-community-815915772.html)

[^presto-fork-impression]: Personally, I looked at the Presto case with sympathy, and said, "Anyone would be tough..."

But actually, when it comes to Embulk, even that was not easy. It was due to the following reasons mentioned above.

1. Embulk can do almost nothing without plugins. The value of the Embulk ecosystem would be nothing if it were spoiled by plugin developers.
2. TD sometimes suggests using Embulk in customer environments.
3. TD states ["Open Source is in our DNA"](https://www.treasuredata.com/opensource/).

In other words, TD depends on "the Embulk ecosystem (including other Embulk plugins)" as a user, and moreover, TD states "Open Source is in our DNA" for ourselves. It would be contradictory to ourselves to disrupt the ecosystem by not maintaing Embulk as open source.

![TD is also a user of non-TD plugins and "the Embulk ecosystem"](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/ca9vxiz43ul6k82uztb7.png)

An ecosystem cannot be sustained unless someone takes the lead, makes decisions, and pays the tax to keep it maintained. TD had not paid the tax to keep the open-source Embulk ecosystem maintained. The ecosystem would simply wither away if no one took the lead, and TD "left everything to the community". The ecosystem would only crash if TD continued to make selfish changes for our own convenience. Also, TD occupies the committer privileges to the core. No one else could pay the tax.

The Embulk ecosystem as open source was thus stuck.

It was like being satisfied with just making the core of a middleware, and not being able to spend any more to maintain the ecosystem on top of it. We might have seen a similar situation in the past, where people were satisfied with just making the hardware, and not being able to foster a software platform on top of it.

TD's business was taking turns this time. I talked to the manager about the possibility of including open source efforts more in the personnel evaluation, but the response was something like, "We miss the open source spirit, but it seems difficult...". Frankly, I felt that we were not at the stage where we could talk about open source.

Embulk v0.10
-------------

Having said that, we could not just upgrade the library versions without the "huge compatibility-breaking" major overhaul mentioned above. It was obvious that we would not even be able to keep the Data Connector operational. We somehow started the major overhaul with such a justification. This was the Embulk v0.10 series, which started almost at the same time as the Covid pandemic (and finally took three years).

The technical details of the Embulk v0.10 series are currently being documented as [EEPs (Embulk Enhancement Proposal)](https://github.com/embulk/embulk/tree/master/docs/eeps). Please take a look if you're interested. (See below.)

This major overhaul went smoothly up to the point where we made a certain amount of changes in the core, [announced the steps to make an Embulk plugin work with the new Embulk](https://dev.embulk.org/topics/get-ready-for-v0.11-and-v1.0.html), and released up-to-date plugins that were officially maintained by the team. Indeed, a little over a year after the start of v0.10, we reached [Embulk v0.10.32](https://github.com/embulk/embulk/releases/tag/v0.10.32), which solved one of the biggest issue of the dependency libraries, [Jackson](https://github.com/FasterXML/jackson).

However, we were not able to make rapid progress from there. We had three main reasons for the slowdown. [^td-tech-talk-2022-blog]

1. We had almost 200 in-house plugins for Data Connector. [^200-plugins]
    * We had to make all the ~200 plugins work with new Embulk because it was "the huge compatibility-breaking major overhaul"
    * Our development speed had (has) limitations, even though the in-house plugins were developed by the Data Connector team, not alone by me alone.
    * This might seem like "just a technical debt / refactoring project" to managers. I talked to them about the need to move forward quickly, as it had show-stopper risks (once we had requests for a new plugin that required an up-to-date library).
    * But of course, we were not able to lower the priorities of incoming requests.
2. The implementation of Data Connector as a framework was deeply coupled to the internal structure of Embulk.
    * If we had changed Embulk's internal structure a little bit, Data Connector could have broken many times for unfathomable reasons, while the open-source Embulk worked perfectly.
3. We had to start working on "a part in Embulk that is not used by Data Connector".
    * It was not easy to justify working on it as my regular weekday job, as mentioned above.

[^200-plugins]: First of all, I wanted to get it started quickly before the number of plugins grew too much...

Finally, it was another year after we released v0.10.32 as open source when we were able to start using v0.10.32 in Data Connector. It took a long time for the team to have the in-house plugins catch up with v0.10.32 (#1), and for me to decouple the deeply-coupled Data Connector and Embulk (#2). [^class-loader-leak]

[^class-loader-leak]: For example, we had a microservice in Data Connector that had a weird architecture of "running Embulk repeatedly in a single Java process as a REST API server". It had been causing memory leaks ([Class Loader Leak](https://www.ibm.com/docs/en/was-nd/9.0.5?topic=cmlp-memory-leaks-in-java-platform-enterprise-edition-applications)) since the early days. We rebuilt the microservice almost from scratch, because it turned out to be impossible to solve the problem with just a "fix up".

We had put off working on "some parts in Embulk that needed to be done for the open-source Embulk to be consistent, which meant that Embulk was useless without those parts, while we wouldn't use those parts in Data Connector" (#3). We pretended we didn't see those parts. But eventually, we had to start working on those parts in addition.

Changes in the organization
============================

My Embulk maintainer duty managed to work "on the side behind Data Connector development" until this v0.10.32 (although it was hard for me and many others). However, after a year of trying to deploy v0.10.32 into Data Connector, this setup broke down completely.

It effectively meant that the TD organization was just a boat anker for the open-source Embulk ecosystem. It was hard for us to say "Open Source is in our DNA". I had to say that TD was being disloyal to the customers that TD was recommending Embulk to. Finally, it was an "unprofitable job" for me personally.

Despite the conflict, after a year of struggle, we overcame the biggest challenge of deploying v0.10.32 + α in Data Connector. Almost at the same time, I was told of an occasion for a team transfer. We had talked about it before for "sometime after the major overhaul". I wanted to try something else, as I had been with Embulk and Data Connector for more than five years at that point. The occasion came to me a little earlier.

I hesitated because we still had (many) unfinished things in Data Connector, but, in the end I accepted "half" of it. This "half" means that I now spend half of my time working on another product in another team in TD, and the other half taking on a role as "maintainer of the open-source Embulk" (independent of Data Connector).

At the same time, I explicitly changed the organization of the open-source Embulk project. The organization was overstretched to keep Embulk maintained. I thought that the transfer could be a good opportunity also for the Embulk project to change its organization.

Split from Data Connector
--------------------------

First of all, I used the team transfer of myself as a good opportunity to split "open-source Embulk maintainers" and "Data Connector developers", supported by managers in terms of personnel measures.

As mentioned above, "the position to maintain the open-source Embulk" and "the position to develop the Data Connector product" often conflict with each other, unfortunately. As long as "the open-source Embulk" is an ecosystem that includes external stakeholders, the conflict will never have a permanent resolution. I, a single personality, had to accommodate the conflict by wearing many hats. [^persona] It was not sustainable for long, both as an individual burden and as the personnel evaluation. It was the first overstretch.

[^persona]: I had the feeling that it could lead to a change in personality if I continued.

Conflicts in a project would be visible by splitting the two in terms of personnel measures. If conflict is inevitable, it would be healthier to make the conflict visible than to force the conflict on individuals. It would also allow us to deal with it as an organization.

Additionally, a committer (me) came to Embulk who can only contribute to the good of "the open-source Embulk ecosystem". It brought a good result for Embulk because we were able to start working on backlogged issues since it was "unused in Data Connector". It was also good for me because my contributions to Embulk are being properly evaluated.

To be clear, this does not mean that the Data Connector team has been excluded from the open-source Embulk community. The Embulk Core Team described below includes a member of the Data Connector team. On the other hand, it would be more beneficial and constructive for both teams to have an explicit discussion between me "maintaining the open-source Embulk" and the team "developing Data Connector".

The Data Connector team would need some more time, experience, and preparation for management and personnel evaluation to behave well as "good contributors". We expect that this new relationship would serve as a preparation and practice period for them.

The Embulk Core Team and EEP
-----------------------------

The situation of the Embulk ecosystem is now better than before the split. However, it was still closed in TD, and only I was still the only one in a position for the Embulk ecosystem. For example, if I took my hands off Embulk, everything would get back. Also, there is no guarantee that the company policy will not change in the future to "Open Source is our junk DNA" instead of "Open Source is in our DNA". (I don't think we will, though.)

The relationship had been skewed, with TD exclusively maintaining the core while leaving the plugin developers, the stakeholders in the ecosystem, behind. Having said that, the ecosystem cannot be sustained without the plugin developers. That was the second overstretch. So the only way is to change the "TD exclusively maintains the core" situation.

This skew was a structural problem. It will never change as long as "TD holds the root (the core) of Embulk". Of course, I will try my best so that the relevant people would be "good contributors", and I believe the TD organization would do so. However, the fundamental solution would be to "explicitly include non-TD plugin developers as 'Embulk insiders'". Otherwise, the risk of the same problem would remain.

That is why the Embulk project explicitly formed the "Core Team". It started from inviting [^involved] major plugin developers outside of TD. Although I am still the only one with the committer privileges to the core as of March 2024, it started as a team to monitor (review) my changes to the core. The idea is to make sure that the committer(s) are "good contributors", and to share the responsibility of the maintainers by formally having the plugin developers as "insiders". We also hope that someone from the Core Team will eventually become a committer / a maintainer.

[^involved]: I might have "involved", or "pulled" them.

![Embulk Core Team](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/b98ukdvo7ll5j0wq2dwi.png)

In addition, we have formulated an explicit process for making major changes to the Embulk core ([EEP: Embulk Enhancement Proposal](https://github.com/embulk/embulk/tree/master/docs/eeps)). This is a process to explicitly document "why it is needed", "who uses it and how", "how the plugin interface changes", and "what steps to take for migration" for approval by the Core Team.

[Link: Embulk maintenance goes open (embulk.org)](https://www.embulk.org/articles/2023/03/10/embulk-maintenance-gets-open.html)

I am also taking actions to leave "retrospective" EEP documents for a number of my changes through v0.10.

Switching the point of view to the Data Connector team, these might seem like changes that block their progress and slow down their development due to external factors. However, the difficulties have always been there already. I had been absorbing the difficulties on my own. We decided with the management that the team should be prepared to "face the open source ecosystem" by themselves, in terms of team management and personnel evaluation. (The details are still a matter of trial and error.)

"Open Source is in our DNA"
============================

Treasure Data has many software engineers who are deeply involved in open source.

* 5 Ruby committers work on TD products (as of March 2024).
* Many have their own open-source projects.
* We make a full use of and contribute to Hive, Presto (Trino), and else, also as a company.

In reality, however, we "as an organization" have not been able to face open source. Maybe it was because each individual was too much exposed to open source on a daily basis, and the idea had slipped from our minds.

Especially for a "middleware with an ecosystem" like Embulk, nothing would move unless someone took responsibility and leadership. Also, leadership does not come for free. If the organization wanted the ecosystem to be sustained, we as an organization had to pay the tax for it.

This Embulk case may have implied that an organization cannot automatically get open source engraved in its DNA just by gathering "individuals who have engraved open source in their DNA". In other words, if we want open source to be engraved in the DNA of our organization, we need to design and build the organization accordingly.

We have also learned that it is not a good idea to hold the initiative (such as committer privileges) of a project (especially open source), and we may want to get stakeholders to be "insiders", especially if the project has "stakeholders" outside of ourselves, and if the relationship is more than just developers and users. This is not just a cosmetic gesture, like "open development is a good thing", but otherwise there is a real risk that the project will become skewed, and not be able to keep going.

This article has shown a case of an open source project (Embulk), with the real actions taken as an employee contributor, the limitations I felt through it, the improvements in the organization, and the changes on the open source side. Through these changes, Embulk has [completed the Embulk v0.10 series, "the major overhaul", and started the next v0.11 series](https://www.embulk.org/articles/2023/04/13/embulk-v0.11-is-coming-soon.html), and is now at the point where we can [plan v1.0 milestones](https://github.com/embulk/embulk/pull/1651).

However, no one knows yet if this was really the right decision in the end. We will continue to think about good ways to move forward and continue the project, both as individuals and as an organization.

This article is just one example of Embulk, one specific project. The way things are done will be different depending on the characteristics of the project and the people involved. I don't think the same approach can be applied to other cases as it is, but I hope this will help someone as a case study.
