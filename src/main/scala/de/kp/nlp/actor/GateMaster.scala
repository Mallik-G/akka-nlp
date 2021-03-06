package de.kp.nlp.actor
/* Copyright (c) 2014 Dr. Krusche & Partner PartG
* 
* This file is part of the Akka-NLP project
* (https://github.com/skrusche63/akka-nlp).
* 
* Akka-NLP is free software: you can redistribute it and/or modify it under the
* terms of the GNU General Public License as published by the Free Software
* Foundation, either version 3 of the License, or (at your option) any later
* version.
* 
* Akka-NLP is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
* A PARTICULAR PURPOSE. See the GNU General Public License for more details.
* You should have received a copy of the GNU General Public License along with
* Akka-NLP. 
* 
* If not, see <http://www.gnu.org/licenses/>.
*/

import akka.actor.{Actor, ActorLogging, Props}

import akka.pattern.ask
import akka.util.Timeout

import akka.actor.{OneForOneStrategy, SupervisorStrategy}
import akka.routing.RoundRobinRouter

import scala.concurrent.duration.DurationInt
import akka.util.Timeout.durationToTimeout

import de.kp.nlp.{GateWrapper,Configuration}

class GateMaster extends Actor with ActorLogging {

  val gate = new GateWrapper()  
  
  /* Load configuration for routers */
  val (time,retries,workers) = Configuration.router   
  
  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries=retries,withinTimeRange = DurationInt(time).minutes) {
    case _ : Exception => SupervisorStrategy.Restart
  }

  val router = context.actorOf(Props(new GateWorker(gate)).withRouter(RoundRobinRouter(workers)), name="gate-router")
    
  def receive = {
    
    case req:String => {

      implicit val ec = context.dispatcher

      val duration = Configuration.actor      
      implicit val timeout:Timeout = DurationInt(duration).second

	  val origin = sender

	  val response = ask(router, req).mapTo[Seq[Map[String,String]]]
      response.onSuccess {
        case result => origin ! result       
      }
      response.onFailure {
        case result => origin ! Seq.empty[Map[String,String]]	      
	  }
     
    }
    
    case _ => log.info("Unknown request")
  
  }

}