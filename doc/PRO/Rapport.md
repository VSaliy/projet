---
title: Commusica
author:
    - tofind
header-includes:
    - \usepackage{fancyhdr}
    - \pagestyle{fancy}
    - \fancyhead[CO,CE]{Commusica}
    - \fancyhead[LO,LE]{}
    - \fancyhead[RO,RE]{}
    - \renewcommand{\contentsname}{Table des mati�res}
    - \lstset{breaklines=true}
    - \lstset{backgroundcolor=\color[RGB]{248,248,248}}
    - \lstset{language=java}
    - \lstset{basicstyle=\small\ttfamily}
    - \lstset{extendedchars=true}
    - \lstset{tabsize=2}
    - \lstset{columns=fixed}
    - \lstset{showstringspaces=false}
    - \lstset{frame=trbl}
    - \lstset{frameround=tttt}
    - \lstset{framesep=4pt}
    - \lstset{numbers=left}
    - \lstset{numberstyle=\tiny\ttfamily}
toc: yes
---

\newpage

# Introduction
Durant le quatri�me semestre de la section TIC de l'HEIG-VD, nous devons effectuer un projet par groupes de cinq ou six personnes, le but �tant de mettre en ?uvre les connaissances que nous avons acquises au long des semestres pr�c�dents � travers un projet cons�quent. Nous devrons prendre conscience des difficult�s li�es au travail de groupe, ainsi qu'apprendre � planifier un travail sur plusieurs mois. Au terme du semestre, nous devons rendre une programme complet et fonctionnel, avec une documentation ad�quate et �tre capables de le pr�senter et le d�fendre.

Dans le cadre du projet, l'�quipe de programmation est compos�e du chef d'�quipe Ludovic Delafontaine, de son rempla�ant Lucas Elisei et des membres David Truan, Thibaut Togue, Yosra Harbaoui et Denise Gemesio.  
Dans ce rapport, nous allons expliquer notre d�marche de travail et les principaux choix d'architecture et de design de code que nous avons choisi. Il sera structur� en partie repr�sentant les principaux paquets de notre applications. Nous d�velopperons plus ou moins les paquets selon leur importance au sein de notre projet.


## Objectif
Le but de notre programme est de proposer une application client-serveur qui permettra aux clients d'envoyer des fichiers musicaux au serveur pour que celui-ci les joue. Il se d�marque d'une simple application de streaming dans le fait que la liste de lecture ne peut �tre chang�e que par les clients par le biais d'un syst�me de votes positifs ou n�gatifs. Ceux-ci permettent � un morceau pr�sent d'�tre plac� plus en avant ou en arri�re dans la liste de lecture. Ceci permet donc � chacun de donner son avis, tout en centralisant la lecture de la musique sur un seul ordinateur. En plus de cela l'application met � disposition les fonctionnalit�s suivantes pour une exp�rience encore plus communautaire:

 +  Vote pour passer au morceau suivant. Lorsqu'une majorit� (plus de 50%) des clients ont vot� pour passer au morceau suivant, la piste en cours de lecture est remplac�e par le morceau qui la suit dans la liste de lecture.
 +  M�me principe de vote pour augment� ou diminuer le volume.
 +  Syst�me de favoris pour permettre aux utilisateurs de sauvegarder les informations (titre, auteur, etc...) en local dans une playlist sp�cifique.

Cette application visera principalement les soir�es avec plusieurs personnes et r�pondra � l'�ternel probl�me de devoir se passer une prise jack ou de devoir se battre pour pouvoir passer un morceau que l'on aime.


# Conception / Architecture
Description  diagramme des cas d'utilisation avec figure. Diagramme UML, la sauvegarde...


# Description technique
utiliser les packages pour la description
Ne pas mettre toutes les classes !


## Gestionnaire de configuration
Nous avons choisi d'impl�menter un gestionnaire de configuration utilisant le fichier commusica.properties pour permettre � l'utilisateur de configurer le programme. Elle donne acc�s aux param�tres suivants :    

 +  SERVER_NAME : choix nom du serveur auquel les participants pourront se connecter
 +  PLAYLIST_NAME : choix du nom de la playlist pour la soir�e
 +  DEBUG : au niveau d�veloppement, choisir ou non d'afficher les logs
 +  DATE_FORMAT : choix du format de la date
 +  VOLUME_STEP : choix du pas d'augmentation et abaissement de la musique
 +  TRACKS_DIRECTORY : choix du chemin relatif o� les chansons seront stock�es
 +  TIME_BEFORE_SESSION_INACTIVE : choix du d�lai d'inactivit� d'une session
 +  TIME_BETWEEN_PLAYLIST_UPDATES : choix du d�lai de mise � jour des playlists et leurs chansons


## Package Core
Pour garder un niveau d'abstraction le plus �lev� possible, nous avons voulu faire transiter toutes les informations venant du r�seau et de actions utilisateurs via l'interface graphique par un contr�leur. Le but �tant d'avoir le m�me point d'entr�e que l'on soit client ou serveur. Pour cela il nous fallait une sorte de contr�leur central qui puisse �tre appel� de la m�me fa�on peut importe le choix de l'identit� client/serveur mais en pouvant avoir des m�thodes distincte selon que l'on choisisse d'�tre l'un ou l'autre. Notre raisonnement nous a men� � nous tourner vers la r�flectivit� offerte par **Java** pour r�soudre ce probl�me. Ce m�canisme permet d'instancier � l'ex�cution des m�thodes en utilisant la m�thode `invoke(Object obj, Object... args)` en ayant comme premier param�tre un String repr�sentant le nom de la m�thode � invoquer et en deuxi�me param�tre un tableau d'`Object` contentant les diff�rents arguments que la m�thode invoqu�e pourra utiliser (voir utilisation dans notre programme plus loin **FIGURE**).  


Il nous fallait maintenant une classe qui puisse jouer le r�le du contr�leur. Nous avons d�velopper les **Core** pour cela qui sont tous dans le paquet *core*.
![Classes principales du package *core*](fr)

### Classes du package
#### Core
Classe statique qui joue le r�le de point d'entr�e. Elle dispose d'un attribut `AbstractCore` qui sera instancier soit en `ClientCore` ou en `ServerCore`. Elle met aussi � disposition des m�thodes statiques qui nous permettrons de les appeler depuis les autres classes du programme.  
Parmi ces m�thodes, la plus importante est la suivante:  

```java
public static String execute(String command, ArrayList<Object> args)
```
 Cette m�thode se contente d'appeler la m�thode du m�me nom de l'instance de `AbstractCore` de la classe et sera appel�e partout ou une action du Core est demand�e.
Elle contient aussi des m�thodes lui permettant de se param�trer comme client ou serveur.

#### ICore
Interface qui d�fini le n�cessaire � un core � savoir des m�thodes permettant d'envoyer des message en Unicast ou en Multicast et une m�thode pour stopper le core. Toutes les classes h�ritant de `AbstarctCore` doivent impl�menter cette interface.

#### AbstractCore
Cette classe abstraite met � disposition les m�thodes pour permettre � ses sous-classes de s'ex�cuter correctement. Contrairement � `Core` cette classe va utiliser la r�flectivit� dans se m�thode execute() comme ceci:  

```java
public synchronized String execute(String command, ArrayList<Object> args) {

    String result = "";

    try {
        Method method = this.getClass().getMethod( command, ArrayList.class);
        result = (String) method.invoke(this, args);
    } catch (NoSuchMethodException e) {
        // Do nothing
    } catch (IllegalAccessException | InvocationTargetException e) {
        LOG.error(e);
    }

    return result;
}
```

On re�oit une commande et un tableau correspondant aux arguments de la m�thode � invoquer. Ensuite le programme essaie de retrouver la m�thode ayant un nom correspondant � la commande envoy�e si elle est disponible dans l'instance de la classe. Si c'est le cas elle va l'invoquer et donc ex�cuter ladite m�thode, sinon une exception est lev�e. C'est gr�ce � cette m�thode que tout prend son sens car on a maintenant une instance d'`AbstractCore` qui est soit `ClientCore` ou `ServerCore` avec une seule m�thode pour en appeler d'autres qui seront-elles impl�ment�es dans les sous-classes de `AbstractCore`.

#### ServerCore et ClientCore
Ces deux classe h�ritant de `AbstractCore` impl�mentant `ICore` sont les parties les plus importantes du projet et c'est ici que la majorit� des actions (transfert de la musique, action � effectuer lors d'un appui dur un bouton, etc...) se fera. Lors de l'envoie des commandes ces classes fonctionnent un peu avec un syst�me d'�tats qui peuvent �tre chang� en recevant des commandes depuis le r�seau ou depuis le code. Elles ont une forte interaction avec les classes s'occupant des �changes r�seau puisque c'est ici que toutes les informations re�ues depuis le r�seau vont passer. Grace � la r�flectivit� offerte par l'`AbstractCore` il est donc extr�mement facile de d�finir de nouvelles m�thodes dans ces classes. Pour cela il faut d�clarer une m�thode portant le nom d'une commande (commandes qui seront toutes list�es dans la classe `ApplicationProtocol`).

### Synth�se du paquet core
Grace � ses classes, nous avons r�gl� le probl�me de contr�leur central par lequel tout transitera. La r�ception des commandes � invoquer sera expliqu�e plus tard dans le chapitre sur le paquet `Network` et lors des explications sur la liaison entre l'interface graphique et le code.


## Package database
La sauvegarde et le chargement font partie des points importants de notre application, car elle a �t� con�ue pour permettre, par exemple, � un utilisateur de sauvegarder les metadatas des chansons qui lui plaisent dans la base de donn�es. Ce package est constitu� essentiellement de la classe **DatabaseManager.java** donc le r�le est d'assurer le CRUD (Create, Read, Update, Delete) de la base des donn�es de notre application  et d'assuer la fermeture de la connexion � celle-ci.

Pour l'impl�mentation, nous avons choisi le framework Hibernate qui simplifie le d�veloppement de l'application Java pour interagir avec la base des donn�es. C'est un outil open source et l�ger.
Un outil ORM (Object Relational Mapping) simplifie la cr�ation, la manipulation et l'acc�s aux donn�es. C'est une technique de programmation qui mappe l'objet aux donn�es stock�es dans la base des donn�es.


## Package file
 Le package **file** a pour r�le d'assurer la gestion des fichiers en interagissant avec le syst�me de fichiers . Il est constitu� de deux classes:  **FileManager** et **FilesFormats**.

La performance du framework Hibernate est rapide car le cache est utilis� en interne dans le cadre hibern�.
Le framework Hibernate offre la possibilit� de cr�er automatiquement les tables de la base des donn�es. Il n'est donc pas n�cessaire de les cr�er manuellement.

 + **FilesFormats**: Vue que notre application supporte trois formats mp3, m4a et wav, la classe permet de d�finit les caract�ristique d'un fichier c'est � dire tous les �l�ments nous permettant de savoir le type du fichier.
 + **FileManager**:
   Cette classe permet de supprimer, stock� et d�termin� le type de fichier.
 Pour retrouver l'extension du fichier nous avons proc�d� de telle mani�r� :

	+ Pour les fichiers mp3, on regarde les 3 premiers bytes depuis le d�but du fichier.
	+ Pour les m4a, on regarde les premiers octets mais en partant du quatri�me octet  depuis le d�but du fichier
	+  Pour les wav, � partir du huiti�me octet depuis le d�but du fichier.

Connaitre le type de fichier nous permettra de traiter que les fichiers support� pas notre plateforme et aussi en termes de s�curit� �viter qu'un utilisateur face planter le serveur en envoyant un fichier qui n'est pas support� par celui-ci.


##  Package media
### Classes du package
#### EphermeralPlaylist

 ![](EphermeralPlaylist.png)

 La classe EphermeralPlaylist repr�sente la playlist en cour de construction c'est � dire playlist en cour de lecture. Cela permet de mettre ajout l'interface graphique lors d'une action d'un �l�ment du playlist. La mise � jour se faire gr�ce au pattern observer � travers la liste **ObservableSortedPlaylistTrackList**, qui joue en m�me temps le r�le d?observable et observe. Elle est observer des tracks de la liste dans le but de changer l'�tat de la playlist en cas de vote et downvote, observable dans le cas o� il envoie des notification lors des mise � jours.Nous avons aussi dans cette classe le champs **delegate** La liste de lecture qui sera enregistr�e dans la base de donn�es pour le suivi de celle-ci.

#### Player

 ![](Player.png)

Comme son nom l'indique, il s'agit d'une classe permettant de r�aliser les action de base sur la musique(play, pause, stop, next, previous). Pour l'impl�mentation on avait le choix entre **Mediaplayer** et **sourceDataLine**, nous avons pr�f�r� utilise **Mediaplayer** pour des raisons suivantes:

 + Facile � impl�menter
 + Peut jouer plus de format que sourceDataLine par exemple mp3 n'est pas support� par sourceDataLine.

 Le concept de javaFX m�dia est bas� sur les entit�s suivantes:

  + **M�dia** media resource, contient des informations sur les m�dias, telles que sa source, sa r�solution et ses m�tadonn�es
  + **M�diaPlayer** le composant cl� fournissant les c�ntroles pour la lecture de m�dia.
  + MediaView permettant de supporter l'animation, la translucidit� et les effets.

Nous avons aussi dans cette classe utilis�e les JavaFX's properties dans le but de mettre � jour de mani�r� automatique l'interface utilisateur lorsqu'une modification se produit.

#### SavedPlaylist

Comme son l'indique elle permet juste de sauvegard� les playlist.

#### Track

![](Track.png)

Cettre classe r�prsent� la musique, elle regroupe tous les informations concernant une musique. Nous remarquons dans cette classe que nous avons trois constructeurs.

  + Le constructeur vide: toutes les classes peersistantes doivent avoir un constructeur par d�faut  pour que hibernate puisse les instancier en utilisant le constructeur **Constructor.newInstance()**.
  +  ```public Track(String id, String title, String artist, String album, Integer length, String uri)```: Qui est un constructeur normale pr�nant en param�tre tous les champs de la classe.

  + ```public Track(AudioFile audioFile)```
   Il permet de cr�er un track � partir d'un fichier audio. Il est utile lorsqu'ont souhaite transf�rer un fichier et souhaitez effectuer un contr�le sur un track au lieu de v�rifier le fichier Audio lui m�me.


##  Package network
Une autre partie importante de notre programme �tait la gestion du r�seau entre les client et le serveur. Nous nous somme longuement pench� sur la question de qui devait avoir quelles responsabilit�s. Notre application demande plusieurs points au niveau du r�seau:

+ Avoir un protocole r�seau qui se base sur des commandes avec arguments.
+ Le serveur doit pouvoir g�rer plusieurs clients mais sans devoir garder une connexion constante entre chaque client et le serveur.
+ Les clients doivent pouvoir avoir une d�couverte des serveurs disponibles.
+ Les serveur doivent pouvoir envoyer une mise � jour de leur liste de lecture actuelle � tous les clients. Ces derniers ne devront traiter que les informations qui viennent du serveur auquel ils sont connect�s.
+ Un client ne doit pas pouvoir upvoter/downvoter plusieurs fois le m�me morceau.

Nous avons d�cid� de partir sur une architecture avec un thread r�ceptionniste `Server` au niveau du serveur qui va attendre une nouvelle connexion d'un client sur son socket et lancer un thread `UnicastClient` qui va s'occuper de la communication avec le client. Cette communication se fait via un socket unicast puisque toutes les informations mise � part la mise � jour de la playlist vont transiter du client au serveur.  
La classe `UnicastClient` va recevoir les commandes venant du r�seau et en renvoyer. Sa force est qu'elle peut �tre utilis�e aussi bien du c�t� serveur que du c�t� client gr�ce � un syst�me de lecture de flux d'entr�e tant que la fin d'une commande n'est pas d�tect�e ou que l'autre partie n'a pas ferm� son socket. Le socket est ferm� lorsque la commande `END_OF_COMMUNICATION` est re�ue.  
Apr�s la r�ception de la ligne `END_OF_COMMAND`, la lecture du flux d'entr�e est arr�t�e et la commande est s�par�e pour en extraire la partie commande et ses diff�rents arguments. **PAS FINI**


##  Package playlist
Le package **playlist** met en ?uvre ce qui a trait � la gestion des playlists, dans notre cas :

  +  Le lien entre une certaine chanson et les playlists dans lesquelles elle se trouve.
  +  La s�lection d'une certaine playlist.
  +  La gestion des upvotes et downvotes concernant les chansons contenues dans une playlist sp�cifique.

### Classes du package
#### PlaylistManager
La classe **PlaylistManager** repr�sente un gestionnaire de playlists et a plusieurs utilit�s :

  +  R�cup�rer la playlist en cours de cr�ation
  +  R�cup�rer les playlists sauvegard�es
  +  R�cup�rer la playlist des favoris
  +  Ajouter/supprimer des chansons � la playlist des favoris
  +  Cr�er/supprimer une playlist
#### PlaylistTrack
La classe **PlaylistTrack** permet non seulement de repr�senter le lien entre une chanson et une playlist mais aussi de conna�tre le nombre de votes de la chanson, ce qui sera ensuite utile au niveau de la classe **VoteComparator** qui organise les chansons dans la playlist selon le nombre de votes. Cela peut �tre fait gr�ce au fait que **PlaylistTrack** met � disposition une variable **votesProperty** � laquelle un observeur a �t� ajout� afin que l'interface graphique se r�organise correctement.
#### PlaylistTrackId
Cette classe permet de cr�er le lien entre une certaine playlist et une chanson. Gr�ce � l'impl�mentation d'un hashcode, on peut se servir de celui-ci afin de v�rifier que la chanson reli�e � la playlist n'existe pas d�j�.

#### VoteComparator
Le comparateur de vote ne poss�de qu'une fonction. Celle-ci sert tout simplement � d�terminer entre deux chansons, laquelle a le plus grand nombre de votes. Cela a �t� cr�� dans le but de r�organiser la playlist en commen�ant par les chansons les plus vot�es.

##  Package et ressources ui
Concernant l'interface graphique, nous avons utilis� la librairie JavaFx. Celle-ci nous a permis de faire usage de l'outil SceneBuilder afin de d�velopper en premier lieu un mock-up qui s'est ensuite d�velopp�, � travers plusieurs �tapes en l'interface graphique que nous avons aujourd'hui. Le fonctionnement JavaFX demande � avoir deux notions qui communiquent entre elles: un ou plusieurs fichiers FXML qui d�finissent l'arrangement de la fen�tre et une ou plusieurs classes Java qui permettent de lancer la fen�tre et communiquer avec ses composants.
Il est donc int�ressant de conna�tre le cheminement que nous avons parcouru jusqu'au r�sultat actuel.
Dans un premier temps, nous avons d�velopp� un fichier FXML gr�ce � SceneBuilder. Gr�ce � celui-ci, nous avons pu apprendre les bons usages FXML. Nous avons ensuite cr�� un fichier Java depuis lequel nous �tions capable lancer la fen�tre au d�marrage du programme. Cependant, le code se d�veloppant devenant de plus en plus important, nous avons pris la d�cision de diviser aussi bien les fichier FXML que les fichier Java en plusieurs sections permettant d'avoir un regard plus pr�cis sur chaque partie de notre impl�mentation.
Ainsi, nous avons aujourd'hui plusieurs classes Java et fichiers FXML qui sont reli�s � leur classe principale **UIController.java** respectivement **main.fxml**.
### Classes du package
La description des classes se fera selon l'ordre des vues dans l'interface graphique, en partant de la vue en haut � gauche pour finir par la vue en bas au centre. Nous allons tout d'abord commencer par la classe principale.
#### UIController
**UIController** est la classe qui permet de lier le reste des classes entre elles. Lorsque le programme est lanc�, c'est cette classe qui est lanc�e. Elle va en premier lieu faire appara�tre une fen�tre demandant � l'utilisateur si celui-ci veut �tre le serveur. Son lancement a lieu dans la fonction **initialize()**. Au moment du choix de l'utilisateur, l'interface annonce au Core quelle configuration a �t� choisie et la fen�tre principale du programme peut �tre lanc�e.
La classe **UIController** permet �galement de fermer la fen�tre proprement lorsque l'utilisateur d�cidera d'arr�ter le programme.
#### PlaylistsListView
*En haut � gauche*
#### TrackListView
*En haut au centre*
##### PlaylistTrackCell
*Dans TrackListView*
#### SettingsView
*En haut � droite*
#### PreviousTrackView
*En bas � gauche*
#### PlayerControlsView
*En bas au milieu*
#### CurrentTrackView
*En bas au milieu*
### Fichiers FXML


##  Package utils
Le package **utils** r�unit tous les utilitaires dont nous avons eu besoin au sein de plusieurs classes et dont l'impl�mentation n'avait aucun sens au sein desdites classes. L'utilit� de chaque classe diff�re alors �norm�ment.
### Classes du package
#### Configuration
Cette classe permet la r�cup�ration des configurations de base du programme. Elle fixe le fichier de configuration que nous avons introduit pr�c�demment, au chapitre **Gestionnaire de configuration** et en tire des informations.
#### EphemeralPlaylistSerializer
Cette classe permet de s�rialiser et d�s�rialiser une playlist en JSON. L'utilit� de cette classe r�side alors principalement dans la communication r�seau.
#### Logger
Cette classe a �t� cr��e uniquement pour assouvir le besoin d'un d�bogueur indiquant dans quelle classe a lieu une action. Des couleurs ont �t� attribu�es aux diff�rentes notifications.

  +  Bleu pour les informations
  +  Rouge pour les erreurs
  +  Vert pour les succ�s
  +  Jaune pour les warnings
L'affichage des logs peut tout � fait �tre d�sactiv� au niveau du fichier de configuration **commusica.properties** en r�glant la valeur de **DEBUG** � 0.
#### Network
Cette classe permet de r�cup�rer toutes les informations basiques de la machine concernant le r�seau. Elle va en outre permettre de r�cup�rer les interfaces disponibles n�cessaires � la connexion � un certain serveur et de configurer le r�seau pour le reste de l'application.
#### ObservableSortedPlaylistTracklist
Cette classe permet de r�cup�rer les informations n�cessaires � l'affichage des chansons dans la playlist en �coute. Cet utilitaire a �t� cr�� afin de pouvoir faciliter la r�cup�ration d'informations depuis les classes mettant en oeuvre l'interface graphique.
#### Serialize
Gr�ce � la librairie Gson de Google, cette classe est utilis�e dans la s�rialisation et d�s�rialisation d'objets.

## Parties manquantes par rapport au cahier des charges

## Tests r�alis�s

## Probl�mes subsistants

## Am�liorations futures

## Planification / organisation

# Conclusion

# Bilan

## Bilan du groupe

## Ludovic
## Lucas
## David
## Thibaut
## Yosra
## Denise

# Glossaire

# Sources
https://blog.axopen.com/2013/11/les-cles-primaires-composees-avec-hibernate-4/
https://vladmihalcea.com/2016/08/01/the-best-way-to-map-a-composite-primary-key-with-jpa-and-hibernate/

# Annexes
## Cahier des charges
## Journal de travail
## Panification initiale et son �volution
