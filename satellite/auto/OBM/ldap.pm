#########################################################################
# OBM           - File : OBM::ldap.pm (Perl Module)                     #
#               - Desc : Librairie Perl pour OBM                        #
#               Les fonctions communes de gestion LDAP                  #
#########################################################################
# Cree le 2003-05-06                                                    #
#########################################################################
# $Id$                  #
#########################################################################
package OBM::ldap;

use OBM::Parameters::common;
use OBM::Parameters::ldapConf;
use Net::LDAP;
use Net::LDAP::Entry;
use Unicode::MapUTF8 qw(to_utf8 from_utf8 utf8_supported_charset);
require Exporter;

@ISA = qw(Exporter);
@EXPORT_function = qw(loadTreeFromDb updateLdap);
@EXPORT = (@EXPORT_function);
@EXPORT_OK = qw();

#
# Necessaire pour le bon fonctionnement du package
$debug=1;


sub makeDN {
    my( $entry, $parentDn ) = @_;

    my $entryDn = $attributeDef->{$entry->{"node_type"}}->{"dn_prefix"}."=".$entry->{"name"};

    if( defined($parentDn) ) {
        $entryDn .= ",".$parentDn;
    }

    return $entryDn;
}


sub initNode {
    my( $ldapStruct, $parentDn ) = @_;

    # On construit le DN de l'entité courante
    $ldapStruct->{"dn"} = makeDN( $ldapStruct, $parentDn );

    # Si il y a des initialisations spécifiques au type
    if( exists($attributeDef->{$ldapStruct->{"node_type"}}->{"init_struct"}) && defined($attributeDef->{$ldapStruct->{"node_type"}}->{"init_struct"}) ) {
        &{$attributeDef->{$ldapStruct->{"node_type"}}->{"init_struct"}}( $ldapStruct, $parentDn );
    }
}


sub loadDbData {
    my( $entry ) = @_;

    for( my $i=0; $i<=$#{$entry->{"data_type"}}; $i++ ) {
        &OBM::toolBox::write_log( "Recuperation des informations de type '".$entry->{"data_type"}->[$i]."', pour le domaine : ".$main::domainList->[$entry->{"domain_id"}]->{"domain_name"}, "W" );

        if( exists($attributeDef->{$entry->{"data_type"}->[$i]}->{"get_db_value"}) && defined($attributeDef->{$entry->{"data_type"}->[$i]}->{"get_db_value"}) ) {
            $entry->{$entry->{"data_type"}->[$i]} = &{$attributeDef->{$entry->{"data_type"}->[$i]}->{"get_db_value"}}( $entry->{"dn"}, $entry->{"domain_id"} );
        }else {
            $entry->{$entry->{"data_type"}->[$i]} = [];
        }

    }

    return $result;
}


sub loadTreeFromDb {
    my( $ldapStruct, $parentDn, $domainId ) = @_;

    # On initialise le noeud courant
    initNode( $ldapStruct, $parentDn );

    &OBM::toolBox::write_log( "Gestion du noeud de type '".$ldapStruct->{"node_type"}."' et de dn : ".$ldapStruct->{"dn"}, "W" );

    # Si le domaine du noeud n'est pas positionné
    if( !exists($ldapStruct->{"domain_id"}) && !defined($domainId) ) {
        # Si il n'y a pas de domaine précisé et que le domaine du noeud n'est
        # pas positionné, on attache les informations au meta-domaine
        $domainId = 0;
    }

    if( !exists($ldapStruct->{"domain_id"}) && defined($domainId) ) {
        $ldapStruct->{"domain_id"} = $domainId;
    }

    # On cré les branches correspondants aux templates pour chacun des domaines
    # sauf pour le domaine '0'
    for( my $i=0; $i<=$#{$ldapStruct->{"template"}}; $i++ ) {
        for( my $j=0; $j<=$#$main::domainList; $j++ ) {
            if( $main::domainList->[$j]->{"meta_domain"} ) {
                # On ne cré pas de structure pour les meta-domaines
                next;
            }

            &OBM::toolBox::write_log( "Creation de la structure pour le domaine '".$main::domainList->[$j]->{"domain_dn"}."'", "W" );
            my $currentDomainBranch = &OBM::toolBox::cloneStruct( $ldapStruct->{"template"}->[$i] );

            # On positionne le nom en fonction du domaine, afin de
            # pouvoir créer le DN
            $currentDomainBranch->{"name"} = $main::domainList->[$j]->{"domain_dn"};
            $currentDomainBranch->{"domain_id"} = $j;

            push( @{$ldapStruct->{"branch"}}, $currentDomainBranch )
        }
    }

    # On récupère les informations nécessaires en BD
    loadDbData( $ldapStruct );

    # On parcours les sous branches
    for( my $i=0; $i<=$#{$ldapStruct->{"branch"}}; $i++ ) {
        my $currentDomainId = undef;
        if( defined( $ldapStruct->{"domain_id"} ) ) {
            $currentDomainId = $ldapStruct->{"domain_id"};
        }

        loadTreeFromDb( $ldapStruct->{"branch"}->[$i], $ldapStruct->{"dn"}, $currentDomainId );
    }
}


sub updateLdap {
    my( $ldapStruct, $ldapConn ) = @_;
    # Flag permettant de savoir si la connexion était établie ou a été
    # établie...
    $ldapStruct->{"connInitiator"} = 0;

    if( !defined( $ldapConn ) ) {
        if( !connectLdapSrv( $ldapStruct ) ) {
            return 1;
        }else {
            $ldapStruct->{"connInitiator"} = 1;
            $ldapConn = $ldapStruct->{"ldap_server"}->{"conn"};
        }
    }

    # On cré ou met a jour l'entree courante
    my $ldapEntry = findDn( $ldapConn, $ldapStruct->{"dn"} );
    if( defined( $ldapEntry ) ) {
        &OBM::toolBox::write_log( "Mise a jour de l'entree '".$ldapStruct->{"dn"}."' - Type '".$ldapStruct->{"node_type"}."'", "W" );
        updateLdapEntry( $ldapStruct, $ldapEntry, $ldapConn );

    }else {
        &OBM::toolBox::write_log( "Creation de l'entree '".$ldapStruct->{"dn"}."' - Type '".$ldapStruct->{"node_type"}."'", "W" );

        if( !createLdapEntry( $ldapStruct, $ldapConn ) ) {
            # si la création echoue, on ne traite pas les sous-types et
            # sous-branches.
            return 0;
        }

    }

    # On met a jour les type de données directement attachés à cette entrée
    if( exists($ldapStruct->{"data_type"}) && defined($ldapStruct->{"data_type"}) ) {
        for( my $i=0; $i<=$#{$ldapStruct->{"data_type"}}; $i++ ) {

            # On cherche les entrées LDAP existantes, et qui ne le sont plus
            &OBM::toolBox::write_log( "Gestion des entrees a supprimer de type '".$ldapStruct->{"data_type"}->[$i]."', de la branche '".$ldapStruct->{"dn"}."'", "W" );
            deleteLdapEntries( $ldapStruct, $ldapStruct->{"data_type"}->[$i], $ldapConn );

            # On s'occupe ensuite des éventuelles créations/mises à jour
            for( my $j=0; $j<=$#{$ldapStruct->{$ldapStruct->{"data_type"}->[$i]}}; $j++ ) {
                if( exists($ldapStruct->{$ldapStruct->{"data_type"}->[$i]}->[$j]->{"ldap_server"}) && defined($ldapStruct->{$ldapStruct->{"data_type"}->[$i]}->[$j]->{"ldap_server"}) ) {
                    # Si la sous-structure comporte des informations de
                    # connexion, on la laisse l'établir
                    updateLdap( $ldapStruct->{$ldapStruct->{"data_type"}->[$i]}->[$j], undef );
                }else {
                    # Sinon on transmet le connecteur actuel
                    updateLdap( $ldapStruct->{$ldapStruct->{"data_type"}->[$i]}->[$j], $ldapConn );
                }
            }
        }
    }

    # On met à jour les sous-branches
    if( exists($ldapStruct->{"branch"}) && defined($ldapStruct->{"branch"}) ) {
        for( my $i=0; $i<=$#{$ldapStruct->{"branch"}}; $i++ ) {
            if( exists($ldapStruct->{"branch"}->[$i]->{"ldap_server"}) && defined($ldapStruct->{"branch"}->[$i]->{"ldap_server"}) ) {
                # Si la sous-structure comporte des informations de connexion,
                # on la laisse l'établir
                updateLdap( $ldapStruct->{"branch"}->[$i], undef );
            }else {
                # Sinon on transmet le connecteur actuel
                updateLdap( $ldapStruct->{"branch"}->[$i], $ldapConn );
            }
        }
    }

    # Si on a établie la connexion, on doit le clore
    if( $ldapStruct->{"connInitiator"} ) {
        disconnectLdapSrv( $ldapStruct );
    }

    return 0;
}


sub connectLdapSrv {
    my( $ldapStruct ) = @_;

    # L'entree courante doit être une branche
    if( !$attributeDef->{$ldapStruct->{"node_type"}}->{"is_branch"} ) {
        &OBM::toolBox::write_log( "Erreur : l'entrée de type '".$ldapStruct->{"node_type"}."' ne peut etablir de connexion a l'annuaire.", "W" );
        return 0;
    }

    # On essaye d'etablir la connexion
    if( !exists($ldapStruct->{"ldap_server"}) || !defined($ldapStruct->{"ldap_server"}) ) {
        &OBM::toolBox::write_log( "Erreur : pas de serveur associe a l'entree : ".$ldapStruct->{"dn"}, "W" );
        return 0;
    }

    if( !exists($ldapStruct->{"ldap_server"}->{"server"}) || !exists($ldapStruct->{"ldap_server"}->{"login"}) ) {
        &OBM::toolBox::write_log( "Erreur : parametres du serveur LDAP incorrect", "W" );

        return 0;
    }

    &OBM::toolBox::write_log( "Connexion a l'annuaire LDAP '".$ldapStruct->{"ldap_server"}->{"server"}."', pour la racine '".$ldapStruct->{"dn"}."'", "W" );

    $ldapStruct->{"ldap_server"}->{"conn"} = Net::LDAP->new(
        $ldapStruct->{"ldap_server"}->{"server"},
        port => "389",
        debug => "0",
        timeout => "60",
        version => "3"
    ) or &OBM::toolBox::write_log( "Echec de connexion a LDAP : $@", "W" ) && return 0;

    if( !defined($ldapStruct->{"ldap_server"}->{"conn"}) ) {
        return 0;
    }

    my $errorCode;
    if( $ldapStruct->{"ldap_server"}->{"login"} ) {
        my @ldapUser;
        findEntity( $ldapStruct, "local", $SYSTEMUSERS, $ldapStruct->{"ldap_server"}->{"login"}, \@ldapUser );

        if( ($#ldapUser >= 0) && ($ldapUser[0]->{"dn"}) && ($ldapUser[0]->{"user_password"}) ) {
            &OBM::toolBox::write_log( "Connexion a l'annuaire en tant que '".$ldapUser[0]->{"dn"}."'", "W" );

            $errorCode = $ldapStruct->{"ldap_server"}->{"conn"}->bind(
                $ldapUser[0]->{"dn"},
                password => $ldapUser[0]->{"user_password"}
            );
        }else {
            &OBM::toolBox::write_log( "Connexion anonyme a l'annuaire LDAP", "W" );
            $errorCode = $ldapStruct->{"ldap_server"}->{"conn"}->bind();
        }

    }else {
        &OBM::toolBox::write_log( "Connexion anonyme a l'annuaire LDAP", "W" );
        $errorCode = $ldapStruct->{"ldap_server"}->{"conn"}->bind();
    }

    if( $errorCode->code ) {
        &OBM::toolBox::write_log( "Echec de connexion : ".$errorCode->error, "W" );
        return 0;
    }else {
        &OBM::toolBox::write_log( "Connexion a l'annuaire LDAP etablie", "W" );
    }

    return 1;
}


sub disconnectLdapSrv {
    my( $ldapStruct ) = @_;

    # L'entree courante doit être une branche
    if( !$attributeDef->{$ldapStruct->{"node_type"}}->{"is_branch"} ) {
        &OBM::toolBox::write_log( "Erreur : l'entrée de type '".$ldapStruct->{"node_type"}."' ne peut etablir de connexion a l'annuaire.", "W" );
        return 0;
    }

    # La connexion doit être initialisée
    if( exists($ldapStruct->{"ldap_server"}->{"conn"}) && defined($ldapStruct->{"ldap_server"}->{"conn"}) ) {
        &OBM::toolBox::write_log( "Deconnexion de l'annuaire '".$ldapStruct->{"dn"}."'", "W" );
        $ldapStruct->{"ldap_server"}->{"conn"}->unbind;
    }

    return 1;
}


sub createLdapEntry {
    my( $ldapStruct, $ldapConn ) = @_;

    my $ldapEntry = Net::LDAP::Entry->new;

    if( !exists( $attributeDef->{$ldapStruct->{"node_type"}}->{"create_ldap"} ) || !defined( $attributeDef->{$ldapStruct->{"node_type"}}->{"create_ldap"}) ) {
        &OBM::toolBox::write_log( "Erreur : la fonction de creation des entitee de type '".$ldapStruct->{"node_type"}."' n'est pas definie.", "W" );
        return 0;
    }

    if( !&{$attributeDef->{$ldapStruct->{"node_type"}}->{"create_ldap"}}( $ldapStruct, $ldapEntry ) ) {
        &OBM::toolBox::write_log( "Erreur : lors de la creation de l'entitee de type '".$ldapStruct->{"node_type"}."'", "W" );
        return 0;
    }

    # On positionne le DN
    $ldapEntry->dn( to_utf8( { -string => $ldapStruct->{"dn"}, -charset => $defaultCharSet } ) );

    my $result = $ldapEntry->update( $ldapConn );

    if( $result->is_error() ) {
        &OBM::toolBox::write_log( "Erreur : ".$result->code." - ".$result->error, "W" );
        return 0;
    }

    return 1;
}


sub updateLdapEntry {
    my( $ldapStruct, $ldapEntry, $ldapConn ) = @_;

    if( !exists( $attributeDef->{$ldapStruct->{"node_type"}}->{"update_ldap"} ) || !defined( $attributeDef->{$ldapStruct->{"node_type"}}->{"update_ldap"}) ) {
        &OBM::toolBox::write_log( "Erreur : la fonction de mise a jour des entitee de type '".$ldapStruct->{"node_type"}."' n'est pas definie.", "W" );
        return 0;
    }

    if( !&{$attributeDef->{$ldapStruct->{"node_type"}}->{"update_ldap"}}( $ldapStruct, $ldapEntry ) ) {
        &OBM::toolBox::write_log( "Pas de mise a jour necessaire", "W" );
        return 0;
    }

    my $result = $ldapEntry->update( $ldapConn );

    if( $result->is_error() ) {
        &OBM::toolBox::write_log( "Erreur : ".$result->code." - ".$result->error, "W" );
        return 0;
    }

    return 1;
}


sub deleteLdapEntries {
    my( $ldapStruct, $type, $ldapConn ) = @_;
    my @ldapEntry = ();

    # Les entitées definies en BD
    if( !exists($ldapStruct->{$type}) || !defined($ldapStruct->{$type}) || ($#{$ldapStruct->{$type}} < 0) ) {
        return 1;
    }
    my $entry = $ldapStruct->{$type};

    # On récupère les entitées définies dans l'annuaire
    findLdapEntity( $ldapStruct->{"dn"}, $type, $ldapConn, \@ldapEntry );

    for( my $i=0; $i<=$#ldapEntry; $i++ ) {
        my $j=0;
        my $found = 0;

        while( !$found && ($j <= $#$entry) ) {
            if( $ldapEntry[$i]->get_value( $main::attributeDef->{$type}->{"dn_prefix"} ) eq $entry->[$j]->{$main::attributeDef->{$type}->{"dn_value"}} ) {
                # L'entité existe
                $found = 1;
            }

            $j++;
        }

        # Si l'entitée LDAP n'est pas trouvée dans la liste des entitées de la
        # BD, on la supprime
        if( !$found ) {
            &OBM::toolBox::write_log( "Suppression de l'entree '".$ldapEntry[$i]->dn."' - Type '".$type."'", "W" );
            deleteLdapEntry( $ldapEntry[$i], $type, $ldapConn ); 
        }
    }

    return 1;
}


sub deleteLdapEntry {
    my( $ldapEntry, $type, $ldapConn ) = @_;
    my $result;

    # Si le type est un type structurel, on détruit l'objet LDAP complet
    if( $main::attributeDef->{$type}->{"structural"} ) {
        $result = $ldapConn->delete( $ldapEntry->dn );
    }

    if( $result->is_error() ) {
        &OBM::toolBox::write_log( "Erreur : ".$result->code." - ".$result->error, "W" );
        return 0;
    }

    return 1;
}


sub findEntity {
    my( $ldapStruct, $searchType, $entityType, $value, $result ) = @_;

    # Par defaut, on fait des recherches locales
    if( !defined( $searchType ) ) {
        $searchType = "local";
    }elsif( (lc($searchType) ne "local") || (lc($searchType) ne "global") ) {
        $searchType = "local";
    }

    # On cherche dans l'element courant
    if( $ldapStruct->{"node_type"} eq $entityType ) {
        if( $ldapStruct->{$attributeDef->{$entityType}->{"dn_value"}} eq $value ) {
            push( @{$result}, $ldapStruct );
        }
    }

    # On cherche dans les éléments attachés
    if( exists($ldapStruct->{"data_type"}) && defined($ldapStruct->{"data_type"}) ) {
        for( my $i=0; $i<=$#{$ldapStruct->{"data_type"}}; $i++ ) {
            if( $ldapStruct->{"data_type"}->[$i] ne $entityType ) {
                next;
            }

            # Si le type correspond, on recherche la valeur dans le champs clé
            if( exists($ldapStruct->{$entityType}) && defined($ldapStruct->{$entityType}) ) {
                for( my $j=0; $j<=$#{$ldapStruct->{$entityType}}; $j++ ) {
                    if( $ldapStruct->{$entityType}->[$j]->{$attributeDef->{$entityType}->{"dn_value"}} eq $value ) {
                        push( @{$result}, $ldapStruct->{$entityType}->[$j] );
                        last;
                    }
                }
            }

            # On a trouve le type, mais pas d'entree correspondante à la
            # recherche : on arrete pour le noeud courant
            last;
        }
    }

    # On part dans les sous branches en tenant compte du type de recherche
    if( exists($ldapStruct->{"branch"}) && defined($ldapStruct->{"branch"}) ) {
        for( my $i=0; $i<=$#{$ldapStruct->{"branch"}}; $i++ ) {
            my $type = $ldapStruct->{"branch"}->[$i]->{"node_type"};

            if( (lc($searchType) eq "local") && ($main::attributeDef->{$type}->{"is_branch"}) && (exists($ldapStruct->{"branch"}->[$i]->{"ldap_server"})) ) {
                next;
            }

            findEntity( $ldapStruct->{"branch"}->[$i], lc($searchType), $entityType, $value, $result ),
        }
    }
}


sub findLdapEntity {
    my( $baseDn, $type, $ldapConn, $ldapEntry ) = @_;

    if( !exists($main::attributeDef->{$type}) ) {
        return 0;
    }

    my $searchFilter = "";
    for( my $i=0; $i<=$#{$main::attributeDef->{$type}->{"objectclass"}}; $i++ ) {
        $searchFilter .= "(objectClass=".$main::attributeDef->{$type}->{"objectclass"}->[$i].")";
    }

    my $result = undef;
    if( defined($searchFilter) && ($searchFilter ne "") ) {
        $result = $ldapConn->search(
            base => to_utf8( { -string => $baseDn, -charset => $defaultCharSet } ),
            filter => to_utf8( { -string => "(&".$searchFilter.")", -charset => $defaultCharSet } ),
            scope => "one"
        );
    }

    if( !defined($result) ) {
        return undef;

    }elsif( ($result->code != 32) && $result->is_error() ) {
        # L'erreur 'No such object' n'est, dans ce cas, pas considérée comme un
        # erreur.
        &OBM::toolBox::write_log( "Probleme lors d'une requête LDAP : ".$result->code." - ".$result->error, "W" );
        return undef;

    }elsif( $result->count < 1 ) {
        return undef;

    }else {
        @{$ldapEntry} = $result->entries();
    }

}


sub findDn {
    my( $ldapConn, $dn ) = @_;

    my $scope="base";

    if( !defined($ldapConn) ) {
        return undef;
    }

    my $result = $ldapConn->search(
                    base => to_utf8( { -string => $dn, -charset => $defaultCharSet } ),   
                    scope => $scope,
                    filter => "(objectclass=*)"
                    );

    if( !defined($result) ) {
        return undef;

    }elsif( ($result->code != 32) && $result->is_error() ) {
        # L'erreur 'No such object' n'est, dans ce cas, pas considérée comme un
        # erreur.
        &OBM::toolBox::write_log( "Probleme lors d'une requête LDAP : ".$result->code." - ".$result->error, "W" );
        return undef;

    }elsif( $result->count != 1 ) {
        return undef;

    }else {
        return $result->entry(0);
    }

    return undef;
}

