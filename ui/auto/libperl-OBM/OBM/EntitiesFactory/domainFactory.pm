package OBM::EntitiesFactory::domainFactory;

$VERSION = '1.0';

$debug = 1;

use 5.006_001;
require Exporter;
use strict;

use OBM::Tools::commonMethods qw(
        _log
        dump
        );
use OBM::EntitiesFactory::commonFactory qw(
        _checkSource
        );
use OBM::Parameters::regexp;


sub new {
    my $class = shift;
    my( $source, $domainId ) = @_;

    my $self = bless { }, $class;

    $self->{'source'} = $source;
    if( !$self->_checkSource() ) {
        return undef;
    }

    if( !defined($domainId) ) {
        $self->_log( 'identifiant de domaine non défini', 3 );
        return undef;
    }
    
    if( ref($domainId) || ($domainId !~ /$regexp_id/) ) {
        $self->_log( 'identifiant de domaine \''.$self->{'domainId'}.'\' incorrect', 3 );
        return undef;
    }

    $self->{'domainId'} = $domainId;
    $self->{'domains'} = undef;
    $self->{'running'} = undef;
    $self->{'currentEntity'} = undef;


    return $self;
}


sub DESTROY {
    my $self = shift;

    $self->_log( 'suppression de l\'objet', 4 );

    $self->_reset();
}


sub _reset {
    my $self = shift;

    $self->_log( 'factory reset', 3 );

    $self->{'domains'} = undef;
    $self->{'running'} = undef;
    $self->{'currentEntity'} = undef;

    return 1;
}


sub isRunning {
    my $self = shift;

    if( $self->{'running'} ) {
        $self->_log( 'la factory est en cours d\'exécution', 4 );
        return 1;
    }

    $self->_log( 'la factory n\'est pas en cours d\'exécution', 4 );

    return 0;
}


sub _start {
    my $self = shift;

    $self->_log( 'debut de traitement', 2 );

    my $domain;
    if( !($domain = $self->_getDomain()) ) {
        $self->_log( 'problème à l\'obtention du domaine d\'identifiant \''.$self->{'domainId'}.'\'', 3 );
        return 0;
    }

    if( !$domain->isGlobal && !$self->_getParentDomain( $domain ) ) {
        $self->_log( 'problème à l\'obtention des domaines pères du domaine d\'identifiant \''.$self->{'domainId'}.'\'', 3 );
        return 0;
    }

    if( !$domain->isGlobal && !$domain->getParent() ) {
        $self->_log( 'problème lors du chargement du domaine père de '.$domain->getDescription(), 3 );
        $self->_log( 'le domaine père (global) doit être créé dans le système avant de pouvoir créer '.$domain->getDescription(), 1 );
        return 0;
    }


    $self->{'running'} = 1;
    return $self->{'running'};
}


sub next {
    my $self = shift;

    $self->_log( 'obtention de l\'entité suivante', 2 );

    if( !$self->isRunning() ) {
        if( !$self->_start() ) {
            $self->_reset();
            return undef;
        }
    }

    $self->{'currentEntity'} = shift( @{$self->{'domains'}} );

    if( !$self->{'currentEntity'} ) {
        $self->_reset();
    }

    return $self->{'currentEntity'};
}


sub _getDomain {
    my $self = shift;

    $self->_log( 'obtention du domaine d\'identifiant \''.$self->{'domainId'}.'\'', 2 );

    require OBM::Tools::obmDbHandler;
    my $dbHandler = OBM::Tools::obmDbHandler->instance();

    if( !$dbHandler ) {
        $self->_log( 'connexion à la base de données impossible', 4 );
        return undef;
    }

    my $domainTable = 'Domain';
    my $sambaTable = 'Samba';
    if( $self->{'source'} =~ /^SYSTEM$/ ) {
        $domainTable = 'P_'.$domainTable;
        $sambaTable = 'P_'.$sambaTable;
    }
    my $queryDomain = 'SELECT   domain_id,
                                domain_global,
                                domain_label,
                                domain_description,
                                domain_name,
                                domain_alias,
                                name.samba_value as samba_domain_name,
                                sid.samba_value as samba_sid,
                                pdc.samba_value as samba_pdc,
                                profile.samba_value as samba_user_profile
                       FROM '.$domainTable.'
                       LEFT JOIN '.$sambaTable.' name ON name.samba_name=\'samba_domain\' AND name.samba_domain_id=domain_id
                       LEFT JOIN '.$sambaTable.' sid ON sid.samba_name=\'samba_sid\' AND sid.samba_domain_id=domain_id
                       LEFT JOIN '.$sambaTable.' pdc ON pdc.samba_name=\'samba_pdc\' AND pdc.samba_domain_id=domain_id
                       LEFT JOIN '.$sambaTable.' profile ON profile.samba_name=\'samba_profile\' AND profile.samba_domain_id=domain_id
                       WHERE domain_id = '.$self->{'domainId'};

    my $sth;
    if( !defined( $dbHandler->execQuery( $queryDomain, \$sth ) ) ) {
        $self->_log( 'chargement des domaines depuis la BD impossible', 3 );
        return undef;
    }

    while( my $domaindesc = $sth->fetchrow_hashref() ) {
        require OBM::Entities::obmDomain;
        unshift( @{$self->{'domains'}}, OBM::Entities::obmDomain->new( $domaindesc ) );
    }

    if( $#{$self->{'domains'}} > 0 ) {
        $self->_log( 'obtention de plusieurs domaines d\'identifiant \''.$self->{'domainId'}.'\'', 3 );
        return undef;
    }elsif( $#{$self->{'domains'}} < 0 ) {
        $self->_log( 'domaine d\'identifiant \''.$self->{'domainId'}.'\' non trouvé', 2 );
        return undef;
    }

    return $self->{'domains'}->[0];
}


sub _getParentDomain {
    my $self = shift;
    my( $childDomain ) = @_;

    $self->_log( 'obtention du domain global', 2 );

    require OBM::Tools::obmDbHandler;
    my $dbHandler = OBM::Tools::obmDbHandler->instance();

    if( !$dbHandler ) {
        $self->_log( 'connexion à la base de données impossible', 4 );
        return 0;
    }

    my $domainTable = 'P_Domain';
    my $sambaTable = 'P_Samba';
    my $queryDomain = 'SELECT   domain_id,
                                domain_global,
                                domain_label,
                                domain_description,
                                domain_name,
                                domain_alias,
                                sid.samba_value as samba_sid,
                                pdc.samba_value as samba_pdc
                       FROM '.$domainTable.'
                       LEFT JOIN '.$sambaTable.' sid ON sid.samba_name=\'samba_sid\' AND sid.samba_domain_id=domain_id
                       LEFT JOIN '.$sambaTable.' pdc ON pdc.samba_name=\'samba_pdc\' AND pdc.samba_domain_id=domain_id
                       WHERE domain_global = 1';

    my $sth;
    if( !defined( $dbHandler->execQuery( $queryDomain, \$sth ) ) ) {
        $self->_log( 'chargement des domaines depuis la BD impossible', 3 );
        return 0;
    }

    while( my $domaindesc = $sth->fetchrow_hashref() ) {
        require OBM::Entities::obmDomain;
        my $current = OBM::Entities::obmDomain->new( $domaindesc );

        if( $childDomain->setParent($current) ) {
            $self->_log( 'père de '.$current->getDescription().' incorrect', 4 );
            return 0;
        }

        $childDomain = $current;
    }


    return 1;
}
